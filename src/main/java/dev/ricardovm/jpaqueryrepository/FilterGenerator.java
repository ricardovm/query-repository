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

import java.lang.reflect.Proxy;
import java.util.Map;

class FilterGenerator {

	@SuppressWarnings("unchecked")
	static <T extends JpaQueryRepository.Filter> T generateImplementation(Class<T> clazz) {
		var handler = new FilterInvocationHandler();

		return (T) Proxy.newProxyInstance(
				clazz.getClassLoader(),
				new Class<?>[]{clazz, FilterGenerator.FilterValues.class},
				handler);
	}

	static Map<String, Object> values(Object filter) {
		if (filter instanceof FilterGenerator.FilterValues) {
			return ((FilterGenerator.FilterValues) filter).filterValues();
		}

		throw new IllegalArgumentException("Filter not supported: " + filter.getClass());
	}

	private interface FilterValues {

		Map<String, Object> filterValues();
	}
}
