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
import jakarta.persistence.criteria.*;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
class QueryState<E> {

	final Class<E> entityClass;
	final EntityManager entityManager;
	final Map<String, FilterEntry> filterEntries;
	final Map<String, Object> filterValues;
	final Map<String, String> fetchEntries;
	final Map<String, SortEntry> sortEntries;
	final CriteriaBuilder criteriaBuilder;

	QueryState(Class<E> entityClass, EntityManager entityManager,
		Map<String, FilterEntry> filterEntries, Map<String, Object> filterValues,
		Map<String, String> fetchEntries, Map<String, SortEntry> sortEntries) {
		this.entityClass = entityClass;
		this.entityManager = entityManager;
		this.filterEntries = filterEntries;
		this.filterValues = filterValues;
		this.fetchEntries = fetchEntries;
		this.sortEntries = sortEntries;
		this.criteriaBuilder = entityManager.getCriteriaBuilder();
	}

	List<Predicate> buildPredicates(Root root, CriteriaQuery<?> criteriaQuery) {
		var predicates = new ArrayList<Predicate>();
		var joins = new HashMap<String, Join>();

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

		return predicates;
	}

	List<Order> buildSortOrders(Root root) {
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

		return orders;
	}

	private Predicate createOperationPredicate(Root root, HashMap<String, Join> joins,
		FilterEntry filterEntry, Object value) {
		Expression predicateField;
		String field = filterEntry.field();

		if (field.contains(".")) {
			String[] parts = field.split("\\.");
			From<?, ?> from = root;

			for (int i = 0; i < parts.length - 1; i++) {
				var joinPath = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
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

	private Predicate addCustomOperation(CriteriaQuery<?> criteriaQuery, Root root,
		FilterEntry filterEntry, Object value) {
		if (filterEntry.customOperation() == null) {
			throw new IllegalStateException("Custom operation not found for filter entry: " +
				filterEntry.field());
		}

		var queryContext = new QueryContext(criteriaBuilder, criteriaQuery, root);
		return filterEntry.customOperation().apply(queryContext, value);
	}
}
