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

/**
 * Interface representing a method that applies a filter with two parameters.
 *
 * @param <F>  the type of the filter that implements JpaQueryRepository.Filter
 * @param <V1> the type of the first parameter to be used with the filter
 * @param <V2> the type of the second parameter to be used with the filter
 */
public interface Filter2ParamsMethod<F extends JpaQueryRepository.Filter, V1, V2> {

	void accept(F filter, V1 value, V2 value2);
}
