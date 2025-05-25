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

import dev.ricardovm.queryrepository.domain.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderRepositoryTest extends BaseJpaTest {

	@Test
	void testFindOrderByStatus() {
		var orderRepository = new OrderRepository(em);
		var orders = orderRepository.query(f -> {
			f.status_in(List.of("SHIPPED", "COMPLETED"));
		}).list();

		assertEquals(2, orders.size());
		assertEquals(1L, orders.get(0).getId());
		assertEquals(3L, orders.get(1).getId());
	}

	@Test
	void testLoadRelatedEntities() {
		var orderRepository = new OrderRepository(em);
		var orders = orderRepository.query(f -> {
			f.status_in(List.of("SHIPPED", "COMPLETED"));
			f.fetchItems();
			f.fetchItemsProduct();
		}).list();

		em.clear();

		assertEquals(2, orders.size());

		var order = orders.get(0);
		assertEquals(1L, order.getId());
		assertEquals(2, order.getItems().size());
		assertEquals("Laptop", order.getItems().get(0).getProduct().getName());
		assertEquals("Headphones", order.getItems().get(1).getProduct().getName());

		order = orders.get(1);
		assertEquals(3L, order.getId());
		assertEquals(3, order.getItems().size());
		assertEquals("Monitor", order.getItems().get(0).getProduct().getName());
		assertEquals("Headphones", order.getItems().get(1).getProduct().getName());
		assertEquals("Smartphone", order.getItems().get(2).getProduct().getName());
	}
}
