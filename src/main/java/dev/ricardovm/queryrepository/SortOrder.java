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
 * Represents an enumeration of sort orders that can be used in query sorting.
 * Each sort order corresponds to a specific direction (ascending or descending).
 */
public enum SortOrder {
	ASC("asc"),
	DESC("desc");

	private final String suffix;

	SortOrder(String suffix) {
		this.suffix = suffix;
	}

	static SortOrder fromSuffix(String suffix) {
		for (SortOrder order : values()) {
			if (order.suffix.equals(suffix)) {
				return order;
			}
		}

		return null;
	}
}
