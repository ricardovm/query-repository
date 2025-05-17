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

import dev.ricardovm.jpaqueryrepository.domain.ProductRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRepositoryTest extends BaseJpaTest {

	@Test
	void testFindById() {
		var productRepository = new ProductRepository(em);
		var product = productRepository.query(f -> {
			f.id(1L);
		}).get();

		assertTrue(product.isPresent());
		assertEquals("Laptop", product.get().getName());
	}

	@Test
	void testFindByDescriptionLike() {
		var productRepository = new ProductRepository(em);
		var products = productRepository.query(f -> {
			f.description_like("%phone%");
		}).list();

		assertEquals(2, products.size());
		assertEquals("Smartphone", products.get(0).getName());
		assertEquals("Headphones", products.get(1).getName());
	}

	@Test
	void testFindByPrice() {
		var productRepository = new ProductRepository(em);
		var products = productRepository.query(f -> {
			f.price_gt(new BigDecimal("700.00"));
		}).list();

		assertEquals(2, products.size());
		assertEquals("Laptop", products.get(0).getName());
		assertEquals("Smartphone", products.get(1).getName());
	}
}
