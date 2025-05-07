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
 * Represents an enumeration of various operations that can be used in query
 * filtering or dynamic query construction. Each operation corresponds to a
 * specific condition or comparison.
 */
public enum Operation {
	EQUALS("eq"),
	GREATER("gt"),
	GREATER_EQUAL("ge"),
	LESS("lt"),
	LESS_EQUAL("le"),
	NOT_EQUALS("ne"),
	CONTAINS("in"),
	NOT_CONTAINS("notIn"),
	IS_NULL("null"),
	NOT_NULL("notNull");

	private String suffix;

	Operation(String suffix) {
		this.suffix = suffix;
	}

	static Operation fromSuffix(String suffix) {
		for (Operation op : values()) {
			if (op.suffix.equals(suffix)) {
				return op;
			}
		}

		return null;
	}
}
