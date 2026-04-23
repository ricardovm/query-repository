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
package dev.ricardovm.queryrepository.domain;

import dev.ricardovm.queryrepository.BaseQueryRepository;
import dev.ricardovm.queryrepository.QueryRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class OrderRepository extends BaseQueryRepository<Order, OrderRepository.Params> {

	public OrderRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Params::id);
		addFilter(Params::status);
		addFilter(Params::status_in);
		addFilter(Params::customerName, "customer.name");

		addEntityFetch(Params::fetchItems);
		addEntityFetch(Params::fetchItemsProduct, "items.product");

		addSortField(Params::sortById_desc);
	}

	@Override
	protected Class<Order> entityClass() {
		return Order.class;
	}

	@Override
	protected Class<Params> queryParamsClass() {
		return Params.class;
	}

	public interface Params extends QueryRepository.Params {
		void id(Long id);

		void status(String status);

		void status_in(List<String> statuses);

		void customerName(String customerName);

		void fetchItems();

		void fetchItemsProduct();

		void sortById_desc();
	}
}
