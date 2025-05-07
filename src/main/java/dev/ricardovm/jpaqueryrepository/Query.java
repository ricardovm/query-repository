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
import java.util.function.Consumer;

public class Query<T, F> {

	private final EntityManager entityManager;
	private final Map<String, FilterEntry> filterEntries;
	private final Map<String, Object> filterValues;

	private final CriteriaBuilder criteriaBuilder;
	private final CriteriaQuery<T> criteriaQuery;
	private final Root<T> root;
	private final List<Predicate> predicates = new ArrayList<>();

	Query(Class<T> entityClass, EntityManager entityManager, Map<String, FilterEntry> filterEntries,
	      Map<String, Object> filterValues) {
		this.entityManager = entityManager;
		this.filterEntries = filterEntries;
		this.filterValues = filterValues;

		this.criteriaBuilder = entityManager.getCriteriaBuilder();
		this.criteriaQuery = criteriaBuilder.createQuery(entityClass);
		this.root = criteriaQuery.from(entityClass);
	}

	public Query<T, F> load(Consumer<F> filter) {
		return this;
	}

	public Optional<T> get() {
		var results = buildQuery().setMaxResults(1).getResultList();

		if (results.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(results.get(0));
	}

	public List<T> list() {
		return buildQuery().getResultList();
	}

	private TypedQuery<T> buildQuery() {
		for (var entry : filterValues.entrySet()) {
			var filterEntry = filterEntries.get(entry.getKey());
			if (filterEntry == null) {
				throw new IllegalStateException("Filter entry '" + entry.getKey() + "' not found");
			}

			if (filterEntry.operation() != null) {
				addOperationPredicate(filterEntry, entry.getValue());
			} else {
				addCustomOperation(filterEntry, entry.getValue());
			}
		}

		criteriaQuery.select(root).where(predicates.toArray(new Predicate[0]));

		return entityManager.createQuery(criteriaQuery);
	}

	private void addOperationPredicate(FilterEntry filterEntry, Object value) {
		Predicate predicate = null;
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

		if (predicate != null) {
			predicates.add(predicate);
		}
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
