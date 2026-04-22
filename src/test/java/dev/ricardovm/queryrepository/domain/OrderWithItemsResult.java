package dev.ricardovm.queryrepository.domain;

import java.util.List;

public class OrderWithItemsResult {

	private final Long id;
	private final List<OrderItem> items;

	public OrderWithItemsResult(Long id, List<OrderItem> items) {
		this.id = id;
		this.items = items;
	}

	public Long getId() {
		return id;
	}

	public List<OrderItem> getItems() {
		return items;
	}
}
