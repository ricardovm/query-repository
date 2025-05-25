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

class FilterEntry {

	private final String field;
	private final Operation operation;
	private final CustomOperation customOperation;

	FilterEntry(String field, Operation operation) {
		this.field = field;
		this.operation = operation;
		this.customOperation = null;
	}

	FilterEntry(String field, CustomOperation customOperation) {
		this.field = field;
		this.operation = null;
		this.customOperation = customOperation;
	}

	public String field() {
		return field;
	}

	public Operation operation() {
		return operation;
	}

	public CustomOperation customOperation() {
		return customOperation;
	}
}
