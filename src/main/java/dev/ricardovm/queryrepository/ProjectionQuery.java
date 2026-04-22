/*
 * Copyright 2026 Ricardo Vaz Mannrich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ricardovm.queryrepository;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Executes a projection query that selects a subset of fields instead of whole entities.
 * Results are either {@code Map<String, Object>} (keyed by column name) or instances of
 * a caller-supplied POJO class constructed column-by-column.
 *
 * <p>Entity associations listed as columns (e.g. {@code "customer"}) are resolved via a
 * LEFT JOIN so they are fully loaded and accessible after the persistence context is cleared.
 *
 * <p>{@code @OneToMany} and {@code @ManyToMany} collection associations (e.g. {@code "items"})
 * are supported: a JOIN FETCH warm-up loads them into the persistence context, then the result
 * is projected in Java. Column order must match the constructor parameter order for typed results.
 *
 * <p>Nested paths may only traverse {@code @ManyToOne} or {@code @OneToOne} associations.
 * Paths through collection associations (e.g. {@code "items.product"}) are not yet supported
 * and will throw {@link IllegalArgumentException}.
 *
 * @param <T> the result type ({@code Map<String, Object>} or a POJO class)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ProjectionQuery<T> {

	private final QueryState state;
	private final Class<T> resultType;
	private final String[] columns;

	ProjectionQuery(QueryState state, Class<T> resultType, String[] columns) {
		this.state = state;
		this.resultType = resultType;
		this.columns = columns;
	}

	/**
	 * Executes the projection query and retrieves a single result as an {@code Optional}.
	 * If a result type is specified, the query will return an instance of the specified type.
	 * Otherwise, the query will return a map representing a row of column-value pairs.
	 *
	 * @return an {@code Optional} containing the result of the query, or empty if none found.
	 */
	public Optional<T> get() {
		var collectionColumns = findCollectionColumns();
		if (!collectionColumns.isEmpty()) {
			state.warmCollections(collectionColumns);
			return projectEntities(buildPlainEntityQuery().setMaxResults(1).getResultList()).stream().findFirst();
		}

		if (resultType != null) {
			return buildTypedQuery().setMaxResults(1).getResultList().stream().findFirst();
		}

		return buildMapQuery().setMaxResults(1).getResultList().stream()
			.map(this::tupleToMap)
			.findFirst()
			.map(m -> (T) m);
	}

	/**
	 * Executes the projection query and retrieves the results as a list.
	 * If a result type is provided, the query will return instances of the specified type.
	 * Otherwise, the query will return a list of maps, where each map corresponds to a row
	 * with column names as keys and their respective values as values.
	 *
	 * @return a list containing the query results.
	 */
	public List<T> list() {
		var collectionColumns = findCollectionColumns();
		if (!collectionColumns.isEmpty()) {
			state.warmCollections(collectionColumns);
			return projectEntities(buildPlainEntityQuery().getResultList());
		}

		if (resultType != null) {
			return buildTypedQuery().getResultList();
		}

		return (List<T>) buildMapQuery().getResultList().stream()
			.map(this::tupleToMap)
			.collect(Collectors.toList());
	}

	private List<String> findCollectionColumns() {
		var entityType = state.entityManager.getMetamodel().entity(state.entityClass);
		var result = new ArrayList<String>();
		for (var column : columns) {
			if (column.contains(".")) continue;
			try {
				var attr = entityType.getAttribute(column);
				var type = attr.getPersistentAttributeType();
				if (type == Attribute.PersistentAttributeType.ONE_TO_MANY
					|| type == Attribute.PersistentAttributeType.MANY_TO_MANY
					|| type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {
					result.add(column);
				}
			} catch (IllegalArgumentException ignored) {
			}
		}

		return result;
	}

	private TypedQuery buildPlainEntityQuery() {
		var cq = state.criteriaBuilder.createQuery(state.entityClass);
		var root = cq.from(state.entityClass);
		applyPredicatesAndSort(cq, root);
		cq.select(root);

		return state.entityManager.createQuery(cq);
	}

	private List<T> projectEntities(List entities) {
		var result = new ArrayList<T>();
		for (var entity : entities) {
			result.add(resultType != null ? constructTyped(entity) : (T) buildMap(entity));
		}

		return result;
	}

	private Map<String, Object> buildMap(Object entity) {
		var map = new LinkedHashMap<String, Object>();
		for (var column : columns) {
			map.put(column, readValue(entity, column));
		}

		return map;
	}

	private T constructTyped(Object entity) {
		var values = new Object[columns.length];
		for (var i = 0; i < columns.length; i++) {
			values[i] = readValue(entity, columns[i]);
		}

		for (var constructor : resultType.getConstructors()) {
			if (constructor.getParameterCount() == columns.length) {
				try {
					return (T) constructor.newInstance(values);
				} catch (InstantiationException | IllegalAccessException |
				         InvocationTargetException e) {
					throw new RuntimeException("Failed to construct " + resultType.getSimpleName(), e);
				}
			}
		}

		throw new IllegalStateException("No constructor with " + columns.length + " parameters found in " + resultType.getSimpleName());
	}

	private Object readValue(Object entity, String column) {
		try {
			var parts = column.split("\\.");
			Object current = entity;
			for (var part : parts) {
				if (current == null) return null;
				var getter = "get" + Character.toUpperCase(part.charAt(0)) + part.substring(1);
				current = current.getClass().getMethod(getter).invoke(current);
			}
			return current;
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Failed to read column '" + column + "'", e);
		}
	}

	private TypedQuery<T> buildTypedQuery() {
		var criteriaQuery = state.criteriaBuilder.createQuery(resultType);
		var root = criteriaQuery.from(state.entityClass);

		criteriaQuery.select(
			state.criteriaBuilder.construct(resultType, buildSelections(root).toArray(new Selection[0])));

		applyPredicatesAndSort(criteriaQuery, root);

		return state.entityManager.createQuery(criteriaQuery);
	}

	private TypedQuery<Tuple> buildMapQuery() {
		var criteriaQuery = state.criteriaBuilder.createTupleQuery();
		var root = criteriaQuery.from(state.entityClass);

		criteriaQuery.multiselect(buildAliasedSelections(root));

		applyPredicatesAndSort(criteriaQuery, root);

		return state.entityManager.createQuery(criteriaQuery);
	}

	private List<Selection<?>> buildSelections(Root root) {
		var selections = new ArrayList<Selection<?>>();
		for (var column : columns) {
			selections.add(resolveColumn(root, column));
		}

		return selections;
	}

	private List<Selection<?>> buildAliasedSelections(Root root) {
		var selections = new ArrayList<Selection<?>>();
		for (var column : columns) {
			selections.add(resolveColumn(root, column).alias(column));
		}

		return selections;
	}

	private Selection<?> resolveColumn(Root root, String column) {
		if (column.contains(".")) {
			var parts = column.split("\\.");
			From<?, ?> from = root;
			for (var i = 0; i < parts.length - 1; i++) {
				var attr = ((ManagedType) from.getModel()).getAttribute(parts[i]);
				if (isCollectionAssociation(attr)) {
					throw new IllegalArgumentException(
						"Column path '" + column + "' traverses collection association '" + parts[i] + "'. " +
							"Nested collection paths are not yet supported.");
				}
				from = from.join(parts[i], JoinType.LEFT);
			}

			return from.get(parts[parts.length - 1]);
		}

		Attribute<?, ?> attr = root.getModel().getAttribute(column);
		if (isEntityAssociation(attr)) {
			return root.join(column, JoinType.LEFT);
		}

		return root.get(column);
	}

	private boolean isEntityAssociation(Attribute<?, ?> attr) {
		var type = attr.getPersistentAttributeType();
		return type == Attribute.PersistentAttributeType.MANY_TO_ONE
			|| type == Attribute.PersistentAttributeType.ONE_TO_ONE;
	}

	private boolean isCollectionAssociation(Attribute<?, ?> attr) {
		var type = attr.getPersistentAttributeType();

		return type == Attribute.PersistentAttributeType.ONE_TO_MANY
			|| type == Attribute.PersistentAttributeType.MANY_TO_MANY
			|| type == Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
	}

	private void applyPredicatesAndSort(CriteriaQuery<?> criteriaQuery, Root root) {
		var predicates = (List<Predicate>) state.buildPredicates(root, criteriaQuery);
		if (!predicates.isEmpty()) {
			criteriaQuery.where(predicates.toArray(new Predicate[0]));
		}

		var orders = (List<Order>) state.buildSortOrders(root);
		if (!orders.isEmpty()) {
			criteriaQuery.orderBy(orders);
		}
	}

	private Map<String, Object> tupleToMap(Tuple tuple) {
		var map = new LinkedHashMap<String, Object>();
		for (var column : columns) {
			map.put(column, tuple.get(column));
		}

		return map;
	}
}
