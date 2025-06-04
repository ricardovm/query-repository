/*
 * Copyright 2025 Ricardo Vaz Mannrich
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

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.*;

/**
 * Represents a query builder class that facilitates dynamic query creation
 * and execution using the JPA Criteria API. The query is defined based on
 * provided filters and custom operations, allowing complex conditions to be
 * applied to fetch entities of type {@code T}.
 *
 * @param <T> the entity type being queried.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Query<T> {

	private final Class<T> entityClass;
	private final EntityManager entityManager;
	private final Map<String, FilterEntry> filterEntries;
	private final Map<String, String> fetchEntries;
	private final Map<String, Object> filterValues;
	private final Map<String, SortEntry> sortEntries;

	private final CriteriaBuilder criteriaBuilder;

	Query(Class<T> entityClass, EntityManager entityManager, Map<String, FilterEntry> filterEntries,
	      Map<String, Object> filterValues, Map<String, String> fetchEntries, Map<String, SortEntry> sortEntries) {
		this.entityClass = entityClass;
		this.entityManager = entityManager;
		this.filterEntries = filterEntries;
		this.filterValues = filterValues;
		this.fetchEntries = fetchEntries;
		this.sortEntries = sortEntries;
		this.criteriaBuilder = entityManager.getCriteriaBuilder();
	}

	/**
	 * Retrieves the first result from the query built based on the provided filters and criteria.
	 *
	 * @return an {@link Optional} containing the first result if it exists,
	 *         or an empty {@link Optional} if the result list is empty.
	 */
	public Optional<T> get() {
		var results = buildQuery(null).setMaxResults(1).getResultList();

		if (results.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(results.get(0));
	}

	/**
	 * Executes the query built based on the applied filters and criteria,
	 * retrieving the result as a list of entities of type {@code T}.
	 *
	 * @return a list of entities matching the specified conditions, or an empty
	 *         list if no results are found.
	 */
	public List<T> list() {
		var resultList = buildQuery(null).getResultList();

		for (var entry : filterValues.entrySet()) {
			if (!fetchEntries.containsKey(entry.getKey())) continue;

			var fetchEntry = fetchEntries.get(entry.getKey());
			buildQuery(fetchEntry).getResultList();
		}

		return resultList;
	}

	private TypedQuery<T> buildQuery(String fetchField) {
		var predicates = new ArrayList<Predicate>();
		var joins = new HashMap<String, Join>();

		var criteriaQuery = criteriaBuilder.createQuery(entityClass);
		var root = criteriaQuery.from(entityClass);

		for (var entry : filterValues.entrySet()) {
			if (fetchEntries.containsKey(entry.getKey())) continue;
			if (sortEntries.containsKey(entry.getKey())) continue;

			var filterEntry = filterEntries.get(entry.getKey());
			if (filterEntry == null) {
				throw new IllegalStateException("Query entry '" + entry.getKey() + "' not found");
			}

			if (filterEntry.operation() != null) {
				predicates.add(createOperationPredicate(root, joins, filterEntry, entry.getValue()));
			} else {
				predicates.add(addCustomOperation(criteriaQuery, root, filterEntry, entry.getValue()));
			}
		}

		Selection selectField = root;
		root.alias("this_");
		if (fetchField != null) {
			selectField = fetchEntity(criteriaQuery, root, fetchField);
		}

		criteriaQuery.select(selectField).where(predicates.toArray(new Predicate[0]));

		var orders = new ArrayList<Order>();

		for (var entry : filterValues.entrySet()) {
			if (!entry.getKey().startsWith("sortBy")) continue;

			var sortEntry = sortEntries.get(entry.getKey());
			if (sortEntry == null) {
				throw new IllegalStateException("Sort entry '" + entry.getKey() + "' not found");
			}

			var path = root.get(sortEntry.field());

			var sortOrder = entry.getValue() instanceof SortOrder ? (SortOrder) entry.getValue() : sortEntry.order();

			if (sortOrder == SortOrder.ASC) {
				orders.add(criteriaBuilder.asc(path));
			} else {
				orders.add(criteriaBuilder.desc(path));
			}
		}

		if (!orders.isEmpty()) {
			criteriaQuery.orderBy(orders);
		}

		return entityManager.createQuery(criteriaQuery);
	}

	private Selection<?> fetchEntity(CriteriaQuery<T> criteriaQuery, Root<T> root, String fetchField) {
		criteriaQuery.distinct(true);

		var fields = fetchField.split("\\.");
		var currentRoot = (From) root;

		var alias = "_";

		for (var i = 0; i < fields.length - 1; i++) {
			var field = fields[i];
			var fetchItem = currentRoot.join(field);
			alias += field + "_";
			fetchItem.alias(alias);
			currentRoot = fetchItem;
		}

		var field = fields[fields.length - 1];
		var fetchItem = (Join) currentRoot.fetch(field, JoinType.INNER);
		alias += field + "_";
		fetchItem.alias(alias);

		return currentRoot;
	}

	private Predicate createOperationPredicate(Root<T> root, HashMap<String, Join> joins, FilterEntry filterEntry, Object value) {
		Expression predicateField;
		String field = filterEntry.field();

		if (field.contains(".")) {
			String[] parts = field.split("\\.");
			From<?, ?> from = root;

			for (int i = 0; i < parts.length - 1; i++) {
				var joinPath = String.join(".", java.util.Arrays.copyOfRange(parts, 0, i + 1));
				var join = joins.get(joinPath);
				if (join == null) {
					join = from.join(parts[i], JoinType.INNER);
					joins.put(joinPath, join);
				}
				from = join;
			}

			predicateField = from.get(parts[parts.length - 1]);
		} else {
			predicateField = root.get(field);
		}

		switch (filterEntry.operation()) {
			case EQUALS:
				return criteriaBuilder.equal(predicateField, value);
			case GREATER:
				return criteriaBuilder.greaterThan(predicateField, (Comparable) value);
			case GREATER_EQUAL:
				return criteriaBuilder.greaterThanOrEqualTo(predicateField, (Comparable) value);
			case LESS:
				return criteriaBuilder.lessThan(predicateField, (Comparable) value);
			case LESS_EQUAL:
				return criteriaBuilder.lessThanOrEqualTo(predicateField, (Comparable) value);
			case NOT_EQUALS:
				return criteriaBuilder.notEqual(predicateField, value);
			case LIKE:
				return criteriaBuilder.like(predicateField, (String) value);
			case NOT_LIKE:
				return criteriaBuilder.notLike(predicateField, (String) value);
			case CONTAINS:
				return predicateField.in((Collection) value);
			case NOT_CONTAINS:
				return criteriaBuilder.not(predicateField.in((Collection) value));
			case IS_NULL:
				return criteriaBuilder.isNull(predicateField);
			case NOT_NULL:
				return criteriaBuilder.isNotNull(predicateField);
			default:
				throw new IllegalStateException("Unexpected value: " + filterEntry.operation());
		}
	}

	private Predicate addCustomOperation(CriteriaQuery<T> criteriaQuery, Root<T> root, FilterEntry filterEntry, Object value) {
		if (filterEntry.customOperation() == null) {
			throw new IllegalStateException("Custom operation not found for filter entry: " +
				filterEntry.field());
		}

		var queryContext = new QueryContext(criteriaBuilder, criteriaQuery, root);
		return filterEntry.customOperation().apply(queryContext, value);
	}
}
