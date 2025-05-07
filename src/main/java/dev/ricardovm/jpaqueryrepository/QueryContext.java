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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Represents a context for query operations using the JPA Criteria API.
 * It encapsulates common components required to build and execute
 * dynamic queries such as {@link CriteriaBuilder}, {@link CriteriaQuery},
 * and {@link Root}. This class acts as a container for these components,
 * enabling access and use in advanced query construction.
 */
public class QueryContext {

	private final CriteriaBuilder criteriaBuilder;
	private final CriteriaQuery<?> criteriaQuery;
	private final Root<?> root;

	public QueryContext(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<?> root) {
		this.criteriaBuilder = criteriaBuilder;
		this.criteriaQuery = criteriaQuery;
		this.root = root;
	}

	public CriteriaBuilder criteriaBuilder() {
		return criteriaBuilder;
	}

	public CriteriaQuery<?> criteriaQuery() {
		return criteriaQuery;
	}

	public Root<?> root() {
		return root;
	}
}
