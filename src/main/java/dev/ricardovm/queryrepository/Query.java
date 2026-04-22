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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

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

	private final QueryState<T> state;

	Query(Class<T> entityClass, EntityManager entityManager, Map<String, FilterEntry> filterEntries,
		Map<String, Object> filterValues, Map<String, String> fetchEntries, Map<String, SortEntry> sortEntries) {
		this.state = new QueryState<>(entityClass, entityManager, filterEntries, filterValues, fetchEntries, sortEntries);
	}

	/**
	 * Retrieves the first result from the query built based on the provided filters and criteria.
	 *
	 * @return an {@link Optional} containing the first result if it exists,
	 * or an empty {@link Optional} if the result list is empty.
	 */
	public Optional<T> get() {
		state.filterValues.keySet().stream()
			.filter(state.fetchEntries::containsKey)
			.sorted()
			.map(state.fetchEntries::get)
			.forEach(fetchEntry -> buildQuery(fetchEntry).setMaxResults(1).getResultList());

		return buildQuery(null).setMaxResults(1).getResultList().stream().findFirst();
	}

	/**
	 * Executes the query built based on the applied filters and criteria,
	 * retrieving the result as a list of entities of type {@code T}.
	 *
	 * @return a list of entities matching the specified conditions, or an empty
	 * list if no results are found.
	 */
	public List<T> list() {
		state.filterValues.keySet().stream()
			.filter(state.fetchEntries::containsKey)
			.sorted()
			.map(state.fetchEntries::get)
			.forEach(fetchEntry -> buildQuery(fetchEntry).getResultList());

		return buildQuery(null).getResultList();
	}

	/**
	 * Calculates the total number of entities that match the specified filters
	 * and criteria in the query.
	 *
	 * @return the total count of entities as a long value
	 */
	public long count() {
		return buildCountQuery().getSingleResult();
	}

	/**
	 * Returns a projection query selecting the given columns. The result type is
	 * {@code Map<String, Object>} where keys are the column names as passed.
	 *
	 * @param columns field paths to select (e.g. {@code "id"}, {@code "customer.name"})
	 * @return a {@link ProjectionQuery} producing maps
	 */
	public ProjectionQuery<Map<String, Object>> columns(String... columns) {
		return new ProjectionQuery<>(state, null, columns);
	}

	/**
	 * Returns a typed projection query selecting the given columns. Each result row
	 * is passed to the matching constructor of {@code type} in column order.
	 *
	 * @param type    the result POJO or record class
	 * @param columns field paths to select
	 * @param <R>     the result type
	 * @return a {@link ProjectionQuery} producing instances of {@code type}
	 */
	public <R> ProjectionQuery<R> columns(Class<R> type, String... columns) {
		return new ProjectionQuery<>(state, type, columns);
	}

	private TypedQuery<T> buildQuery(String fetchField) {
		var criteriaQuery = state.criteriaBuilder.createQuery(state.entityClass);
		var root = criteriaQuery.from(state.entityClass);

		var predicates = state.buildPredicates(root, criteriaQuery);

		Selection selectField = root;
		if (fetchField != null) {
			selectField = fetchEntity(criteriaQuery, root, fetchField);
		}

		if (!predicates.isEmpty()) {
			criteriaQuery.where(predicates.toArray(new Predicate[0]));
		}

		criteriaQuery.select(selectField);

		var orders = state.buildSortOrders(root);
		if (!orders.isEmpty()) {
			criteriaQuery.orderBy(orders);
		}

		return state.entityManager.createQuery(criteriaQuery);
	}

	private TypedQuery<Long> buildCountQuery() {
		var criteriaQuery = state.criteriaBuilder.createQuery(Long.class);
		var root = criteriaQuery.from(state.entityClass);

		var predicates = state.buildPredicates(root, criteriaQuery);

		criteriaQuery.select(state.criteriaBuilder.countDistinct(root));
		if (!predicates.isEmpty()) {
			criteriaQuery.where(predicates.toArray(new Predicate[0]));
		}

		return state.entityManager.createQuery(criteriaQuery);
	}

	private Selection<?> fetchEntity(CriteriaQuery<T> criteriaQuery, Root<T> root, String fetchField) {
		criteriaQuery.distinct(true);

		var fields = fetchField.split("\\.");

		Fetch<?, ?> currentFetch = null;
		From<?, ?> currentFrom = root;

		for (String field : fields) {
			if (currentFetch == null) {
				currentFetch = currentFrom.fetch(field, JoinType.INNER);
			} else {
				currentFetch = currentFetch.fetch(field, JoinType.INNER);
			}
		}

		return root;
	}
}
