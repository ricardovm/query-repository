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
package dev.ricardovm.jpaqueryrepository;

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
 * @param <F> the filter type to define query conditions.
 */
public class Query<T, F> {

	private final EntityManager entityManager;
	private final Map<String, FilterEntry> filterEntries;
	private final Map<String, String> fetchEntries;
	private final Map<String, Object> filterValues;

	private final CriteriaBuilder criteriaBuilder;
	private final CriteriaQuery<T> criteriaQuery;
	private final Root<T> root;
	private final List<Predicate> predicates = new ArrayList<>();

	Query(Class<T> entityClass, EntityManager entityManager, Map<String, FilterEntry> filterEntries,
	      Map<String, Object> filterValues, Map<String, String> fetchEntries) {
		this.entityManager = entityManager;
		this.filterEntries = filterEntries;
		this.filterValues = filterValues;
		this.fetchEntries = fetchEntries;

		this.criteriaBuilder = entityManager.getCriteriaBuilder();
		this.criteriaQuery = criteriaBuilder.createQuery(entityClass);
		this.root = criteriaQuery.from(entityClass);
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
		for (var entry : filterValues.entrySet()) {
			if (fetchEntries.containsKey(entry.getKey())) continue;

			var filterEntry = filterEntries.get(entry.getKey());
			if (filterEntry == null) {
				throw new IllegalStateException("Query entry '" + entry.getKey() + "' not found");
			}

			if (filterEntry.operation() != null) {
				addOperationPredicate(filterEntry, entry.getValue());
			} else {
				addCustomOperation(filterEntry, entry.getValue());
			}
		}

		if (fetchField != null) {
			criteriaQuery.distinct(true);
			Join fetchItem = (Join) root.fetch(fetchField, JoinType.INNER);
			fetchItem.alias(fetchField);
		}

		criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));

		return entityManager.createQuery(criteriaQuery);
	}

	private void addOperationPredicate(FilterEntry filterEntry, Object value) {
		Predicate predicate;
		Expression predicateField = root.get(filterEntry.field());

		switch (filterEntry.operation()) {
			case EQUALS:
				predicate = criteriaBuilder.equal(predicateField, value);
				break;
			case GREATER:
				predicate = criteriaBuilder.greaterThan(predicateField, (Comparable) value);
				break;
			case GREATER_EQUAL:
				predicate = criteriaBuilder.greaterThanOrEqualTo(predicateField, (Comparable) value);
				break;
			case LESS:
				predicate = criteriaBuilder.lessThan(predicateField, (Comparable) value);
				break;
			case LESS_EQUAL:
				predicate = criteriaBuilder.lessThanOrEqualTo(predicateField, (Comparable) value);
				break;
			case NOT_EQUALS:
				predicate = criteriaBuilder.notEqual(predicateField, value);
				break;
			case LIKE:
				predicate = criteriaBuilder.like(predicateField, (String) value);
				break;
			case NOT_LIKE:
				predicate = criteriaBuilder.notLike(predicateField, (String) value);
				break;
			case CONTAINS:
				predicate = predicateField.in((Collection) value);
				break;
			case NOT_CONTAINS:
				predicate = criteriaBuilder.not(predicateField.in((Collection) value));
				break;
			case IS_NULL:
				predicate = criteriaBuilder.isNull(predicateField);
				break;
			case NOT_NULL:
				predicate = criteriaBuilder.isNotNull(predicateField);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + filterEntry.operation());
		}

		predicates.add(predicate);
	}

	private void addCustomOperation(FilterEntry filterEntry, Object value) {
		if (filterEntry.customOperation() == null) {
			throw new IllegalStateException("Custom operation not found for filter entry: " +
				filterEntry.field());
		}

		var queryContext = new QueryContext(criteriaBuilder, criteriaQuery, root);
		predicates.add(filterEntry.customOperation().apply(queryContext, value));
	}
}
