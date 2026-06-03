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
package dev.ricardovm.queryrepository;

import dev.ricardovm.queryrepository.domain.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FetchJtaDetectionTest extends BaseJpaTest {

	private EntityManager createJtaProxy(boolean joinedToTransaction) {
		return (EntityManager) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class[]{EntityManager.class},
			(proxy, method, args) -> {
				if ("getTransaction".equals(method.getName())) {
					throw new IllegalStateException("Not allowed on a JTA managed EntityManager");
				}
				if ("isJoinedToTransaction".equals(method.getName())) {
					return joinedToTransaction;
				}
				try {
					return method.invoke(em, args);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
		);
	}

	@Test
	void testList_fetchJtaWithoutTransaction_throwsIllegalStateException() {
		var jtaEm = createJtaProxy(false);
		var repo = new OrderRepository(jtaEm);

		assertThrows(IllegalStateException.class, () ->
			repo.query(f -> {
				f.id(1L);
				f.fetchItems();
			}).list()
		);
	}

	@Test
	void testGet_fetchJtaWithoutTransaction_throwsIllegalStateException() {
		var jtaEm = createJtaProxy(false);
		var repo = new OrderRepository(jtaEm);

		assertThrows(IllegalStateException.class, () ->
			repo.query(f -> {
				f.id(1L);
				f.fetchItems();
			}).get()
		);
	}

	@Test
	void testStream_fetchJtaWithoutTransaction_throwsIllegalStateException() {
		var jtaEm = createJtaProxy(false);
		var repo = new OrderRepository(jtaEm);

		assertThrows(IllegalStateException.class, () ->
			repo.query(f -> {
				f.id(1L);
				f.fetchItems();
			}).stream().collect(Collectors.toList())
		);
	}

	@Test
	void testList_fetchJtaWithTransaction_works() {
		var jtaEm = createJtaProxy(true);
		var repo = new OrderRepository(jtaEm);
		var orders = repo.query(f -> {
			f.status_in(List.of("SHIPPED", "COMPLETED"));
			f.fetchItems();
			f.fetchItemsProduct();
		}).list();

		em.clear();

		assertEquals(2, orders.size());

		var order1 = orders.stream().filter(o -> o.getId().equals(1L)).findFirst().orElseThrow();
		assertEquals(2, order1.getItems().size());
		assertEquals("Laptop", order1.getItems().get(0).getProduct().getName());
	}
}
