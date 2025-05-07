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

import java.util.function.Consumer;

public abstract class JpaQueryRepository<T, F extends JpaQueryRepository.Filter> {

	public final Query<T, F> query(Consumer<F> filter) {
		F filterImpl = createFilter();
		filter.accept(filterImpl);

		return query(filterImpl);
	}

	public final Query<T, F> query(F filter) {
		var filterValues = FilterGenerator.values(filter);
		filterValues.forEach((key, value) -> {
			System.out.printf("Filter key: %s, value: %s%n", key, value);
		});

		return new Query<>();
	}

	public F createFilter() {
		return FilterGenerator.generateImplementation(filterClass());
	}

	protected abstract void buildCriteria();

	protected <V> void addFilter(FilterMethod<F, V> method) {
		var filter = FilterGenerator.generateImplementation(filterClass());

		method.accept(filter, null);

		var filterValues = FilterGenerator.values(filter);
		var field = filterValues.keySet().iterator().next();

		addFilter(method, field);
	}

	protected <V> void addFilter(FilterMethod<F, V> method, String field) {
		var operationIndex = field.lastIndexOf('_');

		var fieldName = field;
		var operation = Operation.EQUALS;

		if (operationIndex > 0) {
			var operationSuffix = field.substring(operationIndex + 1);
			var operationFromSuffix = Operation.fromSuffix(operationSuffix);

			if (operationFromSuffix != null) {
				operation = operationFromSuffix;
				fieldName = field.substring(0, operationIndex);
			}
		}

		addFilter(method, fieldName, operation);
	}

	protected <V> void addFilter(FilterMethod<F, V> method, String field, Operation operation) {
		System.out.printf("Adding filter: %s[%s]%n", field, operation);
	}

	protected void addFilter(VoidFilterMethod<F> method) {
		var filter = FilterGenerator.generateImplementation(filterClass());

		method.accept((F) filter);

		var filterValues = FilterGenerator.values(filter);
		var field = filterValues.keySet().iterator().next();

		addFilter((F o, Boolean v) -> method.accept(o), field);
	}

	protected <V1, V2> void addFilter(Filter2ParamsMethod<F, V1, V2> filter) {

	}

	protected abstract Class<T> entityClass();

	protected abstract Class<F> filterClass();

	protected interface Filter {
	}
}
