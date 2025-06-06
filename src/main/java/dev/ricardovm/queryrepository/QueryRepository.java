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

	Query<T> query(Consumer<P> query);

	/**
	 * Represents a contract for defining filter behavior within the repository query system.
	 * This interface serves as a marker for parameters types and should be extended by repository-specific
	 * parameters interfaces that declare methods for each supported operation.
	 */
	interface Params {
	}
}
