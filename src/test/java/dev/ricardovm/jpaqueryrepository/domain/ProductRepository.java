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
package dev.ricardovm.jpaqueryrepository.domain;

import dev.ricardovm.jpaqueryrepository.JpaQueryRepository;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

public class ProductRepository extends JpaQueryRepository<Product, ProductRepository.Filter> {

	public ProductRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Filter::id);
		addFilter(Filter::description_like);
		addFilter(Filter::price_gt);
	}

	@Override
	protected Class<Product> entityClass() {
		return Product.class;
	}

	@Override
	protected Class<Filter> filterClass() {
		return Filter.class;
	}

	public interface Filter extends JpaQueryRepository.Filter {

		void id(Long id);
		void description_like(String description);
		void price_gt(BigDecimal price);
	}
}
