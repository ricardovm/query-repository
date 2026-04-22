package dev.ricardovm.queryrepository.domain;

import java.time.Instant;

public class OrderResult {

	private final Long id;
	private final Instant orderDate;
	private final Customer customer;

	public OrderResult(Long id, Instant orderDate, Customer customer) {
		this.id = id;
		this.orderDate = orderDate;
		this.customer = customer;
	}

	public Long getId() {
		return id;
	}

	public Instant getOrderDate() {
		return orderDate;
	}

	public Customer getCustomer() {
		return customer;
	}
}
