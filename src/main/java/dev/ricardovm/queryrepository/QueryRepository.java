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

import java.util.function.Consumer;

/**
 * Interface defining the contract for JPA-based query repositories with type-safe, dynamic filter support.
 *
 * <p>This interface simplifies the creation of type-safe, flexible queries by allowing the definition of filter criteria
 * through a strongly-typed params interface.</p>
 *
 * <p>Implementations must provide {@link BaseQueryRepository#buildCriteria()} to define the available operations,
 * {@link BaseQueryRepository#entityClass()} to specify the entity type, and
 * {@link BaseQueryRepository#queryParamsClass()} to specify the params interface type.</p>
 *
 * <p>The repository supports various filter operations, entity fetching, and sorting capabilities
 * that can be configured during the buildCriteria phase and then used at query time.</p>
 *
 * @param <T> the type of the entity being queried.
 * @param <P> the type of the params used to define query criteria, which extends {@link QueryRepository.Params}.
 */
public interface QueryRepository<T, P extends QueryRepository.Params> {


	/**
	 * Constructs and executes a query using the provided configuration.
	 * This method provides a convenient way to define filter criteria and other operations using a lambda
	 * expression that configures a params instance.
	 *
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * // Find products with price > 100 and sort by name
	 * List<Product> expensiveProducts = productRepository.query(q -> q
	 *     q.price_gt(new BigDecimal("100.00"));
	 *     q.sortByName();
	 * ).list();
	 *
	 * // Find products in specific categories with optional name filter
	 * String nameFilter = getOptionalNameFilter(); // may be null
	 * List<Product> filteredProducts = productRepository.query(q -> {
	 *     q.category_in("Electronics", "Gadgets");
	 *
	 *     if (nameFilter != null && !nameFilter.isEmpty()) {
	 *         q.name_like("%" + nameFilter + "%");
	 *     }
	 *
	 *     q.sortByPrice_desc();
	 * }).list();
	 * }</pre>
	 *
	 * @param query a consumer that defines the configuration for the query.
	 *               This consumer accepts an instance of {@code F}, which is used to
	 *               specify the query's filter criteria based on field values and operations, inclunding fetch
	 *               and sort configurations. .
	 * @return a {@link Query} instance representing the query built with the specified filter criteria.
	 *         The resulting {@link Query} can be further used to retrieve query results,
	 *         such as a list of entities or a single entity.
	 */
	Query<T> query(Consumer<P> query);

	/**
	 * Represents a contract for defining filter behavior within the repository query system.
	 * This interface serves as a marker for parameters types and should be extended by repository-specific
	 * parameters interfaces that declare methods for each supported operation.
	 */
	interface Params {
	}
}
