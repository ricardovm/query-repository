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

import java.lang.reflect.Proxy;
import java.util.Map;

class ParamsGenerator {

	@SuppressWarnings("unchecked")
	static <T extends BaseQueryRepository.Params> T generateImplementation(Class<T> clazz) {
		var handler = new FilterInvocationHandler();

		return (T) Proxy.newProxyInstance(
				clazz.getClassLoader(),
				new Class<?>[]{clazz, ParamsValues.class},
				handler);
	}

	static Map<String, Object> values(Object params) {
		if (params instanceof ParamsValues) {
			return ((ParamsValues) params).paramsValues();
		}

		throw new IllegalArgumentException("Parameter not supported: " + params.getClass());
	}

	private interface ParamsValues {

		Map<String, Object> paramsValues();
	}
}
