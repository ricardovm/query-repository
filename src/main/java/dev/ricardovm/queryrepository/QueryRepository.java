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

import javax.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract class providing a framework for repositories using JPA to handle query building with dynamic filters and
 * parameters.
 *
 * <p>This class simplifies the creation of type-safe, flexible queries by allowing the definition of filter criteria
 * through a strongly-typed params interface.</p>
 *
 * <p>Subclasses must implement {@link #buildCriteria()} to define the available operations,
 * {@link #entityClass()} to specify the entity type, and {@link #queryParamClass()} to specify the params interface type.</p>
 *
 * <p>The repository supports various filter operations, entity fetching, and sorting capabilities
 * that can be configured during the buildCriteria phase and then used at query time.</p>
 *
 * @param <T> the type of the entity being queried.
 * @param <P> the type of the params used to define query criteria, which extends {@link Params}.
 */
public abstract class QueryRepository<T, P extends QueryRepository.Params> {

	private final EntityManager entityManager;

	private final Map<String, FilterEntry> filterEntries = new LinkedHashMap<>();
	private final Map<String, String> fetchEntries = new LinkedHashMap<>();
	private final Map<String, SortEntry> sortEntries = new LinkedHashMap<>();

	/**
	 * Constructs a new QueryRepository with the specified EntityManager.
	 * This constructor initializes the repository and calls {@link #buildCriteria()}
	 * to set up the filter, fetch, and sort configurations.
	 *
	 * @param entityManager the JPA EntityManager to be used for query execution
	 */
	protected QueryRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
		buildCriteria();
	}

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
	public final Query<T> query(Consumer<P> query) {
		P paramsImpl = ParamsGenerator.generateImplementation(queryParamClass());
		query.accept(paramsImpl);
		var paramsValues = ParamsGenerator.values(paramsImpl);

		return new Query<>(entityClass(), entityManager, filterEntries, paramsValues, fetchEntries, sortEntries);
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
	 *     addFilter(Params::name);
	 *     addFilter(Params::price_gt);
	 *     addFilter(Params::category, "category.name");
	 *
	 *     // Add entity fetches
	 *     addEntityFetch(Params::fetchCategory);
	 *
	 *     // Add sort fields
	 *     addSortField(Params::sortByName);
	 *     addSortField(Params::sortByPrice_desc);
	 * }
	 * }</pre>
	 *
	 * <p>The configured criteria will be used when constructing queries via the {@link #query(Consumer)}
	 * method.</p>
	 */
	protected abstract void buildCriteria();

	/**
	 * Adds a filter to the query configuration. This method extracts field and filter operation
	 * from the param method name (if the operation suffix is present or equals if not) and associates the filter
	 * with the corresponding field.
	 *
	 * @param <V>    the type of the value associated with the filter method.
	 * @param method the filter method reference used to define the filter criteria.
	 */
	protected final <V> void addFilter(ParamMethod<P, V> method) {
		var paramName = extractParamName(method);
		var field = extractFieldName(paramName);
		var operation = extractOperation(paramName);
		addFilter(paramName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration. This method extracts and filter operation from the param method
	 * name (if the operation suffix is present or equals if not) and associates the filter with the corresponding
	 * field.
	 *
	 * @param <V>        the type of the value associated with the filter method.
	 * @param method     the filter method reference used to define the filter criteria.
	 * @param field      the actual name of the field in the entity to which the filter will be applied.
	 */
	protected final <V> void addFilter(ParamMethod<P, V> method, String field) {
		var paramName = extractParamName(method);
		var operation = extractOperation(paramName);
		addFilter(paramName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration.
	 *
	 * @param <V>        the type of the value associated with the filter method.
	 * @param method     the filter method reference used to define the filter criteria.
	 * @param field      the actual name of the field in the entity to which the filter will be applied.
	 * @param operation  the operation that defines the type of filter to apply
	 *                   (e.g., equals, greater than, less than, etc.).
	 */
	protected final <V> void addFilter(ParamMethod<P, V> method, String field, Operation operation) {
		var paramName = extractParamName(method);
		addFilter(paramName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration. This method extracts field and filter operation from the param
	 * method name. Since there is no parameter value for `VoidParamMethod`, it's used to define boolean filters,
	 * such as not null checks.
	 *
	 * @param method the filter method reference used to define the filter criteria.
	 */
	protected final void addFilter(VoidParamMethod<P> method) {
		var paramName = extractParamName(method);
		var field = extractFieldName(paramName);
		var operation = extractOperation(paramName);
		addFilter(paramName, field, operation);
	}

	/**
	 * Adds a filter to the query configuration. This method allows for custom filtering logic
	 * by associating a custom operation with a filter method.
	 *
	 * @param <V>             the type of the value associated with the filter
	 * @param method          a functional interface representing the method to extract the parameter and value pair
	 * @param customOperation a custom operation defining the filtering logic
	 */
	protected final <V> void addFilter(ParamMethod<P, V> method, CustomOperation customOperation) {
		var paramName = extractParamName(method);
		addFilter(paramName, customOperation);
	}

	/**
	 * Adds a filter to the query configuration. This method allows for custom filtering logic
	 * by associating a custom operation with a filter method. These two parameters are sent to the CustomOperation
	 * as an array of objects, allowing for flexible filtering based on two values.
	 *
	 * @param <V1>            the type of the first value associated with the filter method.
	 * @param <V2>            the type of the second value associated with the filter method.
	 * @param method          a functional interface representing the method to extract the parameter and value pair
	 * @param customOperation a custom operation defining the filtering logic
	 */
	protected final <V1, V2> void addFilter(Param2ParamsMethod<P, V1, V2> method, CustomOperation customOperation) {
		var paramName = extractParamName(method);
		addFilter(paramName, customOperation);
	}

	/**
	 * Adds an entity fetch operation. This method extracts the field representing the related entity from the
	 * param method name.
	 *
	 * @param method the fetch method reference used to define the fetch operation.
	 * @param <V>    the type parameter representing the value type of the filter method
	 */
	protected final <V> void addEntityFetch(VoidParamMethod<P> method) {
		var paramName = extractParamName(method);
		validateFetchParamName(paramName);

		var field = Character.toLowerCase(paramName.charAt(5)) + paramName.substring(6);

		addEntityFetch(paramName, field);
	}

	/**
	 * Adds an entity fetch operation. This method extracts the field representing the related entity from the
	 * param method name.
	 *
	 * @param method the fetch method reference used to define the fetch operation.
	 * @param field  the name of the entity property or field to be included in the fetch operation.
	 *               This corresponds to a relationship or property within the entity.
	 */
	protected final void addEntityFetch(VoidParamMethod<P> method, String field) {
		var paramName = extractParamName(method);
		validateFetchParamName(paramName);

		addEntityFetch(paramName, field);
	}

	/**
	 * Adds a sort field to the query configuration. This method extracts field and sort order from the param
	 * method name (if the order suffix is present or asc if not) and associates the sort operation
	 * with the corresponding field.
	 *
	 * @param method the sort method reference used to define the filter criteria.
	 */
	protected final void addSortField(VoidParamMethod<P> method) {
		var paramName = extractParamName(method);
		validateSortParamName(paramName);

		var field = Character.toLowerCase(paramName.charAt(6)) + paramName.substring(7);
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

		addSortField(paramName, field, order);
	}

	/**
	 * Adds a sort field to the query configuration. This method extracts the sort order from the param
	 * method name (if the order suffix is present or asc if not) and associates the sort operation
	 * with the corresponding field.
	 *
	 * @param method the sort method reference used to define the filter criteria.
	 * @param field  the actual name of the field in the entity to which the filter will be applied.
	 */
	protected final void addSortField(VoidParamMethod<P> method, String field) {
		var paramName = extractParamName(method);
		validateSortParamName(paramName);

		var order = SortOrder.ASC;
		var orderIndex = paramName.lastIndexOf('_');
		if (orderIndex > 0) {
			var orderSuffix = paramName.substring(orderIndex + 1);
			var orderFromSuffix = SortOrder.fromSuffix(orderSuffix);

			if (orderFromSuffix != null) {
				order = orderFromSuffix;
			}
		}

		addSortField(paramName, field, order);
	}

	/**
	 * Adds a sort field to the query configuration.
	 *
	 * @param method    the sort method reference used to define the filter criteria.
	 * @param field     the actual name of the field in the entity to which the filter will be applied.
	 * @param sortOrder the order to sort the field (ASC or DESC)
	 */
	protected final void addSortField(VoidParamMethod<P> method, String field, SortOrder sortOrder) {
		var paramName = extractParamName(method);
		validateSortParamName(paramName);

		addSortField(paramName, field, sortOrder);
	}

	/**
	 * Adds a sort field to the query configuration. This method extracts field from the param method name
	 * ant gets the sort order from the provided param method parameter.
	 *
	 * @param method the sort method reference used to define the filter criteria, including SortOrder parameter.
	 */
	protected final void addSortField(ParamMethod<P, SortOrder> method) {
		var paramName = extractParamName(method);
		validateSortParamName(paramName);

		var field = Character.toLowerCase(paramName.charAt(6)) + paramName.substring(7);

		addSortField(paramName, field, SortOrder.ASC);
	}

	private void addFilter(String paramName, String field, Operation operation) {
		filterEntries.put(paramName, new FilterEntry(field, operation));
	}

	private void addFilter(String paramName, CustomOperation customOperation) {
		filterEntries.put(paramName, new FilterEntry(paramName, customOperation));
	}

	private String extractFieldName(String paramName) {
		var operationIndex = paramName.lastIndexOf('_');

		if (operationIndex < 0) {
			return paramName;
		}

		var operationSuffix = paramName.substring(operationIndex + 1);
		var operation = Operation.fromSuffix(operationSuffix);

		if (operation != null) {
			return paramName.substring(0, operationIndex);
		}

		return paramName;
	}

	private Operation extractOperation(String paramName) {
		var operationIndex = paramName.lastIndexOf('_');

		if (operationIndex < 0) {
			return Operation.EQUALS;
		}

		var operationSuffix = paramName.substring(operationIndex + 1);
		var operation = Operation.fromSuffix(operationSuffix);

		if (operation != null) {
			return operation;
		}

		return Operation.EQUALS;
	}

	private void addEntityFetch(String paramName, String field) {
		fetchEntries.put(paramName, field);
	}

	private void addSortField(String paramName, String field, SortOrder order) {
		sortEntries.put(paramName, new SortEntry(field, order));
	}

	@SuppressWarnings("unchecked")
	private String extractParamName(Object methodReference) {
		var params = ParamsGenerator.generateImplementation(queryParamClass());

		if (methodReference instanceof ParamMethod) {
			var filterMethod = (ParamMethod<P, ?>) methodReference;
			filterMethod.accept(params, null);
		} else if (methodReference instanceof Param2ParamsMethod) {
			var twoParamsMethod = (Param2ParamsMethod<P, ?, ?>) methodReference;
			twoParamsMethod.accept(params, null, null);
		} else if (methodReference instanceof VoidParamMethod) {
			var voidMethod = (VoidParamMethod<P>) methodReference;
			voidMethod.accept(params);
		}

		var paramsValues = ParamsGenerator.values(params);
		return paramsValues.keySet().iterator().next();
	}

	private void validateFetchParamName(String paramName) {
		if (!paramName.startsWith("fetch")) {
			throw new IllegalArgumentException("Fetch filter name must start with 'fetch'");
		}
	}

	private void validateSortParamName(String paramName) {
		if (!paramName.startsWith("sortBy")) {
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
	 * Retrieves the class type of the params interface used by this repository.
	 * This method is used internally to create dynamic implementations of the params interface
	 * when building queries. Implementations should return the interface type that extends
	 * {@link Params} and defines the filtering methods for this repository.
	 *
	 * <p>The params interface should be defined as an inner interface or a separate interface
	 * that extends {@link Params} and declares methods for all supported operations.</p>
	 *
	 * @return the {@code Class} object of the params interface type {@code P}.
	 * @see Params
	 */
	protected abstract Class<P> queryParamClass();

	/**
	 * Represents a contract for defining filter behavior within the repository query system.
	 * This interface serves as a marker for parameters types and should be extended by repository-specific
	 * parameters interfaces that declare methods for each supported operation.
	 */
	protected interface Params {
	}
}
