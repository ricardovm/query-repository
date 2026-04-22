package dev.ricardovm.queryrepository;

import dev.ricardovm.queryrepository.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionQueryTest extends BaseJpaTest {

	@Test
	void testMapResult_scalarColumns() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.id(1L))
			.columns("id", "status")
			.list();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(2, result.size());
		assertEquals(1L, result.get("id"));
		assertEquals("COMPLETED", result.get("status"));
	}

	@Test
	void testMapResult_manyToOneColumn() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.id(1L))
			.columns("id", "customer")
			.list();

		em.clear();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(1L, result.get("id"));
		var customer = (Customer) result.get("customer");
		assertEquals("John Doe", customer.getName());
	}

	@Test
	void testMapResult_nestedPathColumn() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.id(1L))
			.columns("id", "customer.name")
			.list();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(1L, result.get("id"));
		assertEquals("John Doe", result.get("customer.name"));
	}

	@Test
	void testMapResult_listMultipleResults() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.status_in(List.of("SHIPPED", "COMPLETED")))
			.columns("id", "status")
			.list();

		assertEquals(2, results.size());
		var ids = results.stream().map(m -> (Long) m.get("id")).collect(Collectors.toSet());
		assertTrue(ids.contains(1L));
		assertTrue(ids.contains(3L));
	}

	@Test
	void testMapResult_getSingleResult() {
		var repo = new OrderRepository(em);
		var result = repo.query(f -> f.id(2L))
			.columns("id", "status")
			.get();

		assertTrue(result.isPresent());
		assertEquals("PROCESSING", result.get().get("status"));
	}

	@Test
	void testMapResult_getEmpty() {
		var repo = new OrderRepository(em);
		var result = repo.query(f -> f.id(999L))
			.columns("id")
			.get();

		assertTrue(result.isEmpty());
	}

	@Test
	void testMapResult_listEmpty() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.status("CANCELLED"))
			.columns("id")
			.list();

		assertTrue(results.isEmpty());
	}

	@Test
	void testMapResult_withSort() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> {
			f.status_in(List.of("SHIPPED", "COMPLETED"));
			f.sortById_desc();
		}).columns("id", "status").list();

		assertEquals(2, results.size());
		assertEquals(3L, results.get(0).get("id"));
		assertEquals(1L, results.get(1).get("id"));
	}

	@Test
	void testTypedResult_pojoConstructor() {
		var repo = new OrderRepository(em);
		List<OrderResult> results = repo.query(f -> f.id(1L))
			.columns(OrderResult.class, "id", "orderDate", "customer")
			.list();

		em.clear();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(1L, result.getId());
		assertEquals("John Doe", result.getCustomer().getName());
	}

	@Test
	void testTypedResult_list() {
		var repo = new OrderRepository(em);
		List<OrderResult> results = repo.query(f -> f.status_in(List.of("SHIPPED", "COMPLETED")))
			.columns(OrderResult.class, "id", "orderDate", "customer")
			.list();

		assertEquals(2, results.size());
	}

	@Test
	void testTypedResult_get() {
		var repo = new OrderRepository(em);
		var result = repo.query(f -> f.id(3L))
			.columns(OrderResult.class, "id", "orderDate", "customer")
			.get();

		assertTrue(result.isPresent());
		assertEquals(3L, result.get().getId());
	}

	@Test
	void testMapResult_oneToManyCollection() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.id(1L))
			.columns("id", "items")
			.list();

		em.clear();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(1L, result.get("id"));
		var items = (List<OrderItem>) result.get("items");
		assertEquals(2, items.size());
	}

	@Test
	void testMapResult_collectionWithSort() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> {
			f.status_in(List.of("SHIPPED", "COMPLETED"));
			f.sortById_desc();
		}).columns("id", "items").list();

		assertEquals(2, results.size());
		assertEquals(3L, results.get(0).get("id"));
		assertEquals(1L, results.get(1).get("id"));
		assertEquals(3, ((List<?>) results.get(0).get("items")).size());
		assertEquals(2, ((List<?>) results.get(1).get("items")).size());
	}

	@Test
	void testMapResult_emptyCollection() {
		var repo = new OrderRepository(em);
		List<Map<String, Object>> results = repo.query(f -> f.id(4L))
			.columns("id", "items")
			.list();

		assertEquals(1, results.size());
		var items = (List<?>) results.get(0).get("items");
		assertNotNull(items);
		assertTrue(items.isEmpty());
	}

	@Test
	void testTypedResult_withCollection() {
		var repo = new OrderRepository(em);
		List<OrderWithItemsResult> results = repo.query(f -> f.id(1L))
			.columns(OrderWithItemsResult.class, "id", "items")
			.list();

		em.clear();

		assertEquals(1, results.size());
		var result = results.get(0);
		assertEquals(1L, result.getId());
		assertEquals(2, result.getItems().size());
	}
}
