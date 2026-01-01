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

import dev.ricardovm.queryrepository.domain.Order;
import dev.ricardovm.queryrepository.domain.OrderRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void testLoadRelatedEntitiesWithJoinFilter() {
		var orderRepository = new OrderRepository(em);
		var orders = orderRepository.query(f -> {
			f.customerName("John Doe");
			f.fetchItems();
			f.fetchItemsProduct();
		}).list();

		em.clear();

		assertEquals(1, orders.size());

		var order = orders.get(0);
		assertEquals(1L, order.getId());
		assertEquals(2, order.getItems().size());
		assertEquals("Laptop", order.getItems().get(0).getProduct().getName());
	}

	@Test
	void testLoadRelatedEntitiesOnSingleResult() {
		var orderRepository = new OrderRepository(em);
		var orderOption = orderRepository.query(f -> {
			f.id(1L);
			f.fetchItems();
			f.fetchItemsProduct();
		}).get();

		em.clear();

		assertTrue(orderOption.isPresent());

		var order = orderOption.get();
		assertEquals(1L, order.getId());
		assertEquals(2, order.getItems().size());
		assertEquals("Laptop", order.getItems().get(0).getProduct().getName());
		assertEquals("Headphones", order.getItems().get(1).getProduct().getName());
	}

	@Test
	void testDifferentClassLoadersForInstantiationAndExecution() throws Exception {
		var orderRepository = new OrderRepository(em);

		var testClassesDir = new File("target/test-classes").toURI().toURL();
		var classesDir = new File("target/classes").toURI().toURL();

		var customClassLoader = new URLClassLoader(new URL[]{testClassesDir, classesDir}, getClass().getClassLoader()) {
			@Override
			public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (name.equals("dev.ricardovm.queryrepository.RepositoryRunner") ||
						name.equals("dev.ricardovm.queryrepository.domain.OrderRepository$Params")) {
					synchronized (getClassLoadingLock(name)) {
						Class<?> c = findLoadedClass(name);
						if (c == null) {
							c = findClass(name);
						}
						if (resolve) {
							resolveClass(c);
						}
						return c;
					}
				}
				return super.loadClass(name, resolve);
			}
		};

		var runnerClass = customClassLoader.loadClass("dev.ricardovm.queryrepository.RepositoryRunner");
		var runner = runnerClass.getDeclaredConstructor().newInstance();

		var method = runnerClass.getMethod("run", OrderRepository.class);

		try {
			var orders = (List<Order>) method.invoke(runner, orderRepository);
			assertEquals(2, orders.size());
		} catch (InvocationTargetException e) {
			throw (Exception) e.getCause();
		}
	}
}
