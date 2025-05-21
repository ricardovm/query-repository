package dev.ricardovm.jpaqueryrepository.domain;

import dev.ricardovm.jpaqueryrepository.JpaQueryRepository;

import javax.persistence.EntityManager;
import java.util.List;

public class OrderRepository extends JpaQueryRepository<Order, OrderRepository.Filter> {

	public OrderRepository(EntityManager em) {
		super(em);
	}

	@Override
	protected void buildCriteria() {
		addFilter(Filter::status);
		addFilter(Filter::status_in);
	}

	@Override
	protected Class<Order> entityClass() {
		return Order.class;
	}

	@Override
	protected Class<Filter> filterClass() {
		return Filter.class;
	}

	public interface Filter extends JpaQueryRepository.Filter {
		void status(String status);
		void status_in(List<String> statuses);
	}
}
