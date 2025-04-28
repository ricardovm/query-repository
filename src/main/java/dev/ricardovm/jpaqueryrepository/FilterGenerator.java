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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class FilterGenerator {

	@SuppressWarnings("unchecked")
	static <T extends JpaQueryRepository.Filter> T generateImplementation(Class<T> clazz) {
		var handler = new InvocationHandler() {

			private final Map<Object, Object> values = new HashMap<>();

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				var methodName = method.getName();

				if (methodName.equals("filterValues") && method.getParameterCount() == 0) {
					return values;
				}

				if (method.getReturnType().equals(Void.TYPE)) {
					Object value;

					if (args != null && args.length == 1) {
						value = args[0];
					} else if (args != null && args.length > 1) {
						value = Arrays.asList(args);
					} else {
						value = true;
					}

					values.put(methodName, value);
					System.out.printf("Setting %s to %s%n", methodName, Arrays.toString(args));
					return null;
				}

				throw new UnsupportedOperationException("Method not supported: " + method);
			}
		};

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
