package dev.ricardovm.queryrepository.domain;

import dev.ricardovm.queryrepository.QueryRepository;

import javax.persistence.EntityManager;
import java.util.List;

public class OrderRepository extends QueryRepository<Order, OrderRepository.Filter> {

	public OrderRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Filter::status);
		addFilter(Filter::status_in);

		addEntityFetch(Filter::fetchItems);
		addEntityFetch(Filter::fetchItemsProduct, "items.product");
	}

	@Override
	protected Class<Order> entityClass() {
		return Order.class;
	}

	@Override
	protected Class<Filter> filterClass() {
		return Filter.class;
	}

	public interface Filter extends QueryRepository.Filter {
		void status(String status);
		void status_in(List<String> statuses);

		void fetchItems();
		void fetchItemsProduct();
	}
}
