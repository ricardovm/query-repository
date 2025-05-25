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
 * This interface represents a method for applying a filter of type {@code F}
 * to control or modify the data retrieval process. The filter is expected
 * to be a subtype of {@link QueryRepository.Filter}.
 *
 * @param <F> the type of filter, which must extend {@link QueryRepository.Filter}
 */
public interface VoidFilterMethod<F extends QueryRepository.Filter> {

	void accept(F filter);
}
