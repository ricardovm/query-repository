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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract class providing a framework for repositories using JPA to handle query building with dynamic filters.
 * This class simplifies the creation of type-safe, flexible queries by allowing the definition of filter criteria
 * through a strongly-typed filter interface.
 *
 * <p>Subclasses must implement {@link #buildCriteria()} to define the available filter operations,
 * {@link #entityClass()} to specify the entity type, and {@link #filterClass()} to specify the filter interface type.</p>
 *
 * <p>The repository supports various filter operations, entity fetching, and sorting capabilities
 * that can be configured during the buildCriteria phase and then used at query time.</p>
 *
 * @param <T> the type of the entity being queried.
 * @param <F> the type of the filter used to define query criteria, which extends {@link JpaQueryRepository.Filter}.
 */
public abstract class JpaQueryRepository<T, F extends JpaQueryRepository.Filter> {

	private final EntityManager entityManager;

	private final Map<String, FilterEntry> filterEntries = new LinkedHashMap<>();
	private final Map<String, String> fetchEntries = new LinkedHashMap<>();
	private final Map<String, SortEntry> sortEntries = new LinkedHashMap<>();

	/**
	 * Constructs a new JpaQueryRepository with the specified EntityManager.
	 * This constructor initializes the repository and calls {@link #buildCriteria()}
	 * to set up the filter, fetch, and sort configurations.
	 *
	 * @param entityManager the JPA EntityManager to be used for query execution
	 */
	protected JpaQueryRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
		buildCriteria();
	}

	/**
	 * Constructs and executes a query using the provided filter configuration.
	 * This method provides a convenient way to define filter criteria using a lambda expression
	 * that configures a filter instance.
	 *
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * // Find products with price > 100 and sort by name
	 * List<Product> expensiveProducts = productRepository.query(filter -> filter
	 *     .price_gt(new BigDecimal("100.00"))
	 *     .sortByName()
	 * ).getResultList();
	 *
	 * // Find products in specific categories with optional name filter
	 * String nameFilter = getOptionalNameFilter(); // may be null
	 * List<Product> filteredProducts = productRepository.query(filter -> {
	 *     filter.category_in(Arrays.asList("Electronics", "Gadgets"));
	 *
	 *     if (nameFilter != null && !nameFilter.isEmpty()) {
	 *         filter.name_like("%" + nameFilter + "%");
	 *     }
	 *
	 *     filter.sortByPrice_desc();
	 * }).getResultList();
	 * }</pre>
	 *
	 * @param filter a consumer that defines the filter configuration for the query.
	 *               This consumer accepts an instance of {@code F}, which is used to
	 *               specify the query's filter criteria based on field values and operations.
	 * @return a {@link Query} instance representing the query built with the specified filter criteria.
	 *         The resulting {@link Query} can be further used to retrieve query results,
	 *         such as a list of entities or a single entity.
	 * @see #query(Filter)
	 */
	public final Query<T> query(Consumer<F> filter) {
		F filterImpl = createFilter();
		filter.accept(filterImpl);

		return query(filterImpl);
	}

	/**
	 * Constructs and executes a query using the provided filter criteria.
	 * The filter is applied by processing the specified instance of the filter type {@code F}.
	 * This method extracts values from the filter instance and uses them to build a query
	 * based on the filter entries, fetch entries, and sort entries configured in this repository.
	 *
	 * @param filter an instance of {@code F} that defines the filter criteria
	 *               used to customize the query. The filter should specify
	 *               the field values and operations for the query's conditions.
	 * @return a {@code Query<T>} instance representing the constructed query.
	 *         The returned {@code Query} can be used to retrieve results,
	 *         such as a list or a single entity, based on the filter criteria.
	 */
	public final Query<T> query(F filter) {
		var filterValues = FilterGenerator.values(filter);
		return new Query<>(entityClass(), entityManager, filterEntries, filterValues, fetchEntries, sortEntries);
	}

	/**
	 * Creates and returns an instance of the filter type {@code F}.
	 * The filter instance is generated dynamically and is used to define
	 * query filter criteria through its configurable fields and methods.
	 *
	 * @return a dynamically generated implementation of the filter type {@code F}.
	 * This instance provides methods to specify filter conditions for building queries.
	 */
	public F createFilter() {
		return FilterGenerator.generateImplementation(filterClass());
	}

	/**
	 * Defines an abstract method to configure the query criteria for this repository.
	 * This method is called during repository initialization and must be implemented by subclasses
	 * to define all available filter operations, entity fetches, and sort fields.
	 *
	 * <p>Implementation should use the various addFilter, addEntityFetch, and addSortField methods
	 * to register all supported query operations. For example:</p>
	 *
	 * <pre>{@code
	 * protected void buildCriteria() {
	 *     // Add filters for various fields
	 *     addFilter(Filter::name);
	 *     addFilter(Filter::price_gt);
	 *     addFilter(Filter::category, "category.name");
	 *
	 *     // Add entity fetches
	 *     addEntityFetch(Filter::fetchCategory);
	 *
	 *     // Add sort fields
	 *     addSortField(Filter::sortByName);
	 *     addSortField(Filter::sortByPrice_desc);
	 * }
	 * }</pre>
	 *
	 * <p>The configured criteria will be used when constructing queries via the {@link #query(Consumer)}
	 * or {@link #query(Filter)} methods.</p>
	 */
	protected abstract void buildCriteria();

	/**
	 * Adds a filter to the query configuration by generating an implementation of the filter type
	 * and applying the specified filter method. This method extracts field and filter operation
	 * from the generated filter instance and associates the filter method with the corresponding field.
	 *
	 * @param <V>    the type of the value associated with the filter method.
	 * @param method the filter method used to define the filter criteria. It accepts a filter instance
	 *               and a value, enabling the customization of filter logic for the query.
	 */
	protected final <V> void addFilter(FilterMethod<F, V> method) {
		var filterName = extractFilterName(method);
		var field = extractFieldName(filterName);
		var operation = extractOperation(filterName);
		addFilter(filterName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration by analyzing the specified filterName and determining the
	 * appropriate filter operation. This method extracts the operation suffix from the filterName name if present,
	 * and associates the filter method with the determined filterName and operation.
	 *
	 * @param <V>        the type of the value associated with the filter method.
	 * @param method     the filter method used to define the filter criteria. It specifies how the filter
	 *                   instance and value should be processed.
	 * @param field      the actual name of the field in the entity to which the filter will be applied.
	 */
	protected final <V> void addFilter(FilterMethod<F, V> method, String field) {
		var filterName = extractFilterName(method);
		var operation = extractOperation(filterName);
		addFilter(filterName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration. This method integrates the provided filter method,
	 * field name, and operation to define filtering criteria used in query execution.
	 *
	 * @param <V>        the type of the value associated with the filter method.
	 * @param method     the filter method used to define the filter criteria. This method specifies
	 *                   how the filter instance and value should be processed.
	 * @param field      the actual name of the field in the entity to which the filter will be applied.
	 * @param operation  the operation that defines the type of filter to apply
	 *                   (e.g., equals, greater than, less than, etc.).
	 */
	protected final <V> void addFilter(FilterMethod<F, V> method, String field, Operation operation) {
		var filterName = extractFilterName(method);
		addFilter(filterName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration by generating an implementation of the filter type
	 * and applying the specified filter method. This method extracts the field and filter operation
	 * from the generated filter instance and associates the filter method with the corresponding field.
	 *
	 * @param method the filter method to define the filter criteria. It accepts a filter instance
	 *               and enables the customization of filter logic for the query.
	 */
	protected final void addFilter(VoidFilterMethod<F> method) {
		var filterName = extractFilterName(method);
		var field = extractFieldName(filterName);
		var operation = extractOperation(filterName);
		addFilter(filterName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration. This method generates an implementation
	 * of the filter type and applies the specified filter method. The filter criteria
	 * are defined based on the provided custom operation.
	 *
	 * @param <V>             the type of the value associated with the filter method.
	 * @param method          the filter method used to define the filter criteria. It accepts
	 *                        a filter instance and a value, enabling the customization of
	 *                        filter logic for the query.
	 * @param customOperation the custom operation that defines the filter to apply. Allows
	 *                        custom logic for constructing filter predicates dynamically.
	 */
	protected final <V> void addFilter(FilterMethod<F, V> method, CustomOperation customOperation) {
		var filterName = extractFilterName(method);
		addFilter(filterName, customOperation);
	}

	/**
	 * Adds a filter with two parameters to the query configuration. This method integrates
	 * the provided filter method and custom operation to define filtering logic for query execution.
	 * A filter instance is dynamically generated and configured through the specified method.
	 *
	 * @param <V1>            the type of the first value associated with the filter method.
	 * @param <V2>            the type of the second value associated with the filter method.
	 * @param method          the filter method used to define the filter criteria. This method
	 *                        accepts a filter instance and two values to process the filter logic.
	 * @param customOperation the custom operation that defines the filter to apply. Allows custom
	 *                        filtering logic based on context and value.
	 */
	protected final <V1, V2> void addFilter(Filter2ParamsMethod<F, V1, V2> method, CustomOperation customOperation) {
		var filterName = extractFilterName(method);
		addFilter(filterName, customOperation);
	}

	/**
	 * Adds an entity fetch operation by extracting and validating the filter name
	 * and associating it with a corresponding entity field.
	 *
	 * @param method the VoidFilterMethod instance representing the filter method for entity fetch
	 * @param <V>    the type parameter representing the value type of the filter method
	 */
	protected final <V> void addEntityFetch(VoidFilterMethod<F> method) {
		var filterName = extractFilterName(method);
		validateFetchFilterName(filterName);

		var field = Character.toLowerCase(filterName.charAt(5)) + filterName.substring(6);

		addEntityFetch(filterName, field);
	}

	/**
	 * Adds an entity fetch configuration to the query construction process.
	 * This method links a field in the entity to a fetch-related filter method, enabling
	 * the inclusion of related entities in the query results.
	 *
	 * @param <V>    the type of the value associated with the fetch method.
	 * @param method the fetch-related filter method used to determine the fetch criteria.
	 *               It operates on a filter instance and configures fetch logic.
	 * @param field  the name of the entity property or field to be included in the fetch operation.
	 *               This corresponds to a relationship or property within the entity.
	 */
	protected final <V> void addEntityFetch(VoidFilterMethod<F> method, String field) {
		var filterName = extractFilterName(method);
		validateFetchFilterName(filterName);

		addEntityFetch(filterName, field);
	}

	/**
	 * Adds a sort field to the query configuration by extracting and validating the filter name
	 * and associating it with a corresponding entity field.
	 *
	 * @param method the VoidFilterMethod instance representing the filter method for sorting
	 */
	protected final void addSortField(VoidFilterMethod<F> method) {
		var filterName = extractFilterName(method);
		validateSortFilterName(filterName);

		var field = Character.toLowerCase(filterName.charAt(6)) + filterName.substring(7);
		var order = SortOrder.ASC;

		var orderIndex = field.lastIndexOf('_');
		if (orderIndex > 0) {
			var orderSuffix = field.substring(orderIndex + 1);
			var orderFromSuffix = SortOrder.fromSuffix(orderSuffix);

			if (orderFromSuffix != null) {
				order = orderFromSuffix;
				field = field.substring(0, orderIndex);
			}
		}

		addSortField(filterName, field, order);
	}

	/**
	 * Adds a sort field to the query configuration.
	 *
	 * @param method the VoidFilterMethod instance representing the filter method for sorting
	 * @param field  the name of the entity property or field to be sorted
	 */
	protected final void addSortField(VoidFilterMethod<F> method, String field) {
		var filterName = extractFilterName(method);
		validateSortFilterName(filterName);

		var order = SortOrder.ASC;
		var orderIndex = filterName.lastIndexOf('_');
		if (orderIndex > 0) {
			var orderSuffix = filterName.substring(orderIndex + 1);
			var orderFromSuffix = SortOrder.fromSuffix(orderSuffix);

			if (orderFromSuffix != null) {
				order = orderFromSuffix;
			}
		}

		addSortField(filterName, field, order);
	}

	/**
	 * Adds a sort field to the query configuration.
	 *
	 * @param method    the VoidFilterMethod instance representing the filter method for sorting
	 * @param field     the name of the entity property or field to be sorted
	 * @param sortOrder the order to sort the field (ASC or DESC)
	 */
	protected final void addSortField(VoidFilterMethod<F> method, String field, SortOrder sortOrder) {
		var filterName = extractFilterName(method);
		validateSortFilterName(filterName);

		addSortField(filterName, field, sortOrder);
	}

	/**
	 * Adds a sort field to the query configuration using a method reference that takes a SortOrder parameter.
	 * The SortOrder parameter will be used when the query is executed.
	 *
	 * @param method the FilterMethod instance representing the filter method for sorting that takes a SortOrder parameter
	 */
	protected final void addSortField(FilterMethod<F, SortOrder> method) {
		var filterName = extractFilterName(method);
		validateSortFilterName(filterName);

		var field = Character.toLowerCase(filterName.charAt(6)) + filterName.substring(7);

		addSortField(filterName, field, SortOrder.ASC);
	}

	private void addFilter(String filterName, String field, Operation operation) {
		filterEntries.put(filterName, new FilterEntry(field, operation));
	}

	private void addFilter(String filterName, CustomOperation customOperation) {
		filterEntries.put(filterName, new FilterEntry(filterName, customOperation));
	}

	private String extractFieldName(String filterName) {
		var operationIndex = filterName.lastIndexOf('_');

		if (operationIndex < 0) {
			return filterName;
		}

		var operationSuffix = filterName.substring(operationIndex + 1);
		var operation = Operation.fromSuffix(operationSuffix);

		if (operation != null) {
			return filterName.substring(0, operationIndex);
		}

		return filterName;
	}

	private Operation extractOperation(String filterName) {
		var operationIndex = filterName.lastIndexOf('_');

		if (operationIndex < 0) {
			return Operation.EQUALS;
		}

		var operationSuffix = filterName.substring(operationIndex + 1);
		var operation = Operation.fromSuffix(operationSuffix);

		if (operation != null) {
			return operation;
		}

		return Operation.EQUALS;
	}

	private void addEntityFetch(String filterName, String field) {
		fetchEntries.put(filterName, field);
	}

	private void addSortField(String filterName, String field, SortOrder order) {
		sortEntries.put(filterName, new SortEntry(field, order));
	}

	@SuppressWarnings("unchecked")
	private String extractFilterName(Object methodReference) {
		var filter = FilterGenerator.generateImplementation(filterClass());

		if (methodReference instanceof FilterMethod) {
			var filterMethod = (FilterMethod<F, ?>) methodReference;
			filterMethod.accept(filter, null);
		} else if (methodReference instanceof Filter2ParamsMethod) {
			var twoParamsMethod = (Filter2ParamsMethod<F, ?, ?>) methodReference;
			twoParamsMethod.accept(filter, null, null);
		} else if (methodReference instanceof VoidFilterMethod) {
			var voidMethod = (VoidFilterMethod<F>) methodReference;
			voidMethod.accept(filter);
		}

		var filterValues = FilterGenerator.values(filter);
		return filterValues.keySet().iterator().next();
	}

	private void validateFetchFilterName(String filterName) {
		if (!filterName.startsWith("fetch")) {
			throw new IllegalArgumentException("Fetch filter name must start with 'fetch'");
		}
	}

	private void validateSortFilterName(String filterName) {
		if (!filterName.startsWith("sortBy")) {
			throw new IllegalArgumentException("Sort filter name must start with 'sortBy'");
		}
	}

	/**
	 * Retrieves the class type of the entity represented by this repository.
	 * This method is used internally to create and execute queries against the correct entity type.
	 * Implementations should return the concrete entity class that this repository manages.
	 *
	 * @return the {@code Class} object of the entity type {@code T}.
	 */
	protected abstract Class<T> entityClass();

	/**
	 * Retrieves the class type of the filter interface used by this repository.
	 * This method is used internally to create dynamic implementations of the filter interface
	 * when building queries. Implementations should return the interface type that extends
	 * {@link Filter} and defines the filtering methods for this repository.
	 *
	 * <p>The filter interface should be defined as an inner interface or a separate interface
	 * that extends {@link Filter} and declares methods for all supported filter operations.</p>
	 *
	 * @return the {@code Class} object of the filter interface type {@code F}.
	 * @see Filter
	 */
	protected abstract Class<F> filterClass();

	/**
	 * Represents a contract for defining filter behavior within the repository query system.
	 * This interface serves as a marker for filter types and should be extended by repository-specific
	 * filter interfaces that declare methods for each supported filter operation.
	 */
	protected interface Filter {
	}
}
