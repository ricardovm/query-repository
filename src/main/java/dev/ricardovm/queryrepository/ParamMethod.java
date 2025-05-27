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

/**
 * Represents a method of applying a quary parameter to a value in the context of a
 * defined query and repository mechanism.
 *
 * @param <P> the type of the params, extending QueryRepository.Params
 * @param <V> the type of the value to be filtered
 */
public interface ParamMethod<P extends QueryRepository.Params, V> {

	void accept(P params, V value);
}
