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

import javax.persistence.criteria.Predicate;

/**
 * Represents a functional interface for defining custom operations
 * to generate dynamic {@link Predicate} instances for queries.
 * <p>
 * Custom implementations of this interface are intended to provide
 * specific behavior for constructing query predicates based on customized
 * logic or requirements. This allows for flexible integration of
 * non-standard query operations when building criteria-based queries.
 */
public interface CustomOperation {

	Predicate apply(QueryContext context, Object value);
}
