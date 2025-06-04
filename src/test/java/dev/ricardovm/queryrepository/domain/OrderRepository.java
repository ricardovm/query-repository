package dev.ricardovm.queryrepository.domain;

import dev.ricardovm.queryrepository.QueryRepository;

import javax.persistence.EntityManager;
import java.util.List;

public class OrderRepository extends QueryRepository<Order, OrderRepository.Params> {

	public OrderRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Params::status);
		addFilter(Params::status_in);
		addFilter(Params::customerName, "customer.name");

		addEntityFetch(Params::fetchItems);
		addEntityFetch(Params::fetchItemsProduct, "items.product");
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
		void status(String status);
		void status_in(List<String> statuses);
		void customerName(String customerName);

		void fetchItems();
		void fetchItemsProduct();
	}
}
