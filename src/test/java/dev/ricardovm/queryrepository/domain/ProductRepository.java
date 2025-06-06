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
import dev.ricardovm.queryrepository.SortOrder;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.math.BigDecimal;

public class ProductRepository extends BaseQueryRepository<Product, ProductRepository.Params> {

	public ProductRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Params::id);
		addFilter(Params::description_like);
		addFilter(Params::categoryName, "category.name");
		addFilter(Params::price_gt);
		addFilter(Params::orderMinimumQuantity_exists, (ctx, value) -> {
			var minimumQuantity = (Integer) value;

			Subquery<Integer> subquery = ctx.criteriaQuery().subquery(Integer.class);
			Root<OrderItem> subRoot = subquery.from(OrderItem.class);

			subquery.select(ctx.criteriaBuilder().literal(1))
				.where(ctx.criteriaBuilder().greaterThanOrEqualTo(subRoot.get("quantity"), minimumQuantity),
					ctx.criteriaBuilder().equal(subRoot.get("product"), ctx.root()));

			return ctx.criteriaBuilder().exists(subquery);
		});
		addFilter(Params::orderQuantityAndUnitPrice_exists, (ctx, value) -> {
			var params = (Object[]) value;
			var quantity = (Integer) params[0];
			var price = (BigDecimal) params[1];

			Subquery<Integer> subquery = ctx.criteriaQuery().subquery(Integer.class);
			Root<OrderItem> subRoot = subquery.from(OrderItem.class);

			subquery.select(ctx.criteriaBuilder().literal(1))
				.where(ctx.criteriaBuilder().equal(subRoot.get("product"), ctx.root()),
					ctx.criteriaBuilder().equal(subRoot.get("quantity"), quantity),
					ctx.criteriaBuilder().equal(subRoot.get("unitPrice"), price));

			return ctx.criteriaBuilder().exists(subquery);
		});

		addSortField(Params::sortById);
		addSortField(Params::sortByName);
		addSortField(Params::sortByPrice);
		addSortField(Params::sortByPrice_desc);
	}

	@Override
	protected Class<Product> entityClass() {
		return Product.class;
	}

	@Override
	protected Class<Params> queryParamsClass() {
		return Params.class;
	}

	public interface Params extends QueryRepository.Params {

		void id(Long id);
		void description_like(String description);
		void categoryName(String categoryName);
		void price_gt(BigDecimal price);
		void orderMinimumQuantity_exists(Integer minimumQuantity);
		void orderQuantityAndUnitPrice_exists(Integer minimumQuantity, BigDecimal price);

		void sortById();
		void sortByName(SortOrder sortOrder);
		void sortByPrice();
		void sortByPrice_desc();
	}
}
