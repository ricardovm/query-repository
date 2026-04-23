/*
 * Copyright 2026 Ricardo Vaz Mannrich
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
package dev.ricardovm.queryrepository.domain;

import java.util.List;

public class OrderWithItemsResult {

	private final Long id;
	private final List<OrderItem> items;

	public OrderWithItemsResult(Long id, List<OrderItem> items) {
		this.id = id;
		this.items = items;
	}

	public Long getId() {
		return id;
	}

	public List<OrderItem> getItems() {
		return items;
	}
}
