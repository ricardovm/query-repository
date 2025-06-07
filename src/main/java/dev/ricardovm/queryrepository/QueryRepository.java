/**
 * Abstract class providing a framework for repositories using JPA to handle query building with dynamic filters and
 * parameters.
 *
 * <p>This class simplifies the creation of type-safe, flexible queries by allowing the definition of filter criteria
 * through a strongly-typed params interface.</p>
 *
 * <p>Subclasses must implement {@link #buildCriteria()} to define the available operations,
 * {@link #entityClass()} to specify the entity type, and {@link #queryParamsClass()} to specify the params interface type.</p>
 *
 * <p>The repository supports various filter operations, entity fetching, and sorting capabilities
 * that can be configured during the buildCriteria phase and then used at query time.</p>
 *
 * @param <T> the type of the entity being queried.
 * @param <P> the type of the params used to define query criteria, which extends {@link dev.ricardovm.queryrepository.QueryRepository.Params}.
 */
package dev.ricardovm.queryrepository;

import java.util.function.Consumer;

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
