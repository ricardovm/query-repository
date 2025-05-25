# JPA Query Repository

A lightweight Java library that simplifies working with JPA Criteria API by providing a fluent, type-safe interface for building and executing JPA queries.

## Features

- Type-safe query building with method references
- Automatic filter generation based on method naming conventions
- Support for complex filtering operations (equals, greater than, less than, like, contains, etc.)
- Easy fetching of related entities
- Fluent API for building and executing queries
- No external dependencies beyond JPA

## Installation

Add the following dependency to your Maven project:

```xml
<dependency>
    <groupId>dev.ricardovm</groupId>
    <artifactId>jpa-query-repository</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

## Usage

### 1. Create an entity class

```java
@Entity
public class Order {
    @Id
    private Long id;
    private String status;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;

    // getters and setters
}
```

### 2. Create a repository class

Extend `JpaQueryRepository` with your entity type and a custom filter interface:

```java
public class OrderRepository extends JpaQueryRepository<Order, OrderRepository.Filter> {

    public OrderRepository(EntityManager em) {
        super(em);
    }

    @Override
    protected void buildCriteria() {
        // Define filters
        addFilter(Filter::status);
        addFilter(Filter::status_in);

        // Define entity fetches
        addEntityFetch(Filter::fetchItems);
        addEntityFetch(Filter::fetchItemsProduct, "items.product");

        // Define sort fields
        addSortField(Filter::sortById);
        addSortField(Filter::sortByDate);
        addSortField(Filter::sortByTotal_desc);
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

        void fetchItems();
        void fetchItemsProduct();

        void sortById();
        void sortByDate(SortOrder sortOrder);
        void sortByTotal_desc();
    }
}
```

### 3. Use the repository to query data

```java
// Create repository instance
OrderRepository orderRepository = new OrderRepository(entityManager);

// Get a list
List<Order> orders = orderRepository.query(f -> {
    f.status_in(List.of("SHIPPED", "COMPLETED"));
    f.fetchItems();
    f.fetchItemsProduct();
    f.sortByTotal_desc();
}).list();

// Or get a single result
Optional<Order> order = orderRepository.query(f -> {
    f.status("SHIPPED");
}).get();
```

## Filter Naming Conventions

The library supports automatic operation detection based on method name suffixes:

| Suffix    | Operation       | Example Method          | Description                                |
|-----------|-----------------|-------------------------|--------------------------------------------|
| (none)    | EQUALS          | `status(String)`        | Field equals the provided value            |
| _eq       | EQUALS          | `status_eq(String)`     | Field equals the provided value            |
| _gt       | GREATER         | `price_gt(Double)`      | Field greater than the provided value      |
| _ge       | GREATER_EQUAL   | `price_ge(Double)`      | Field greater than or equal to value       |
| _lt       | LESS            | `price_lt(Double)`      | Field less than the provided value         |
| _le       | LESS_EQUAL      | `price_le(Double)`      | Field less than or equal to value          |
| _ne       | NOT_EQUALS      | `status_ne(String)`     | Field not equal to the provided value      |
| _like     | LIKE            | `name_like(String)`     | Field matches the LIKE pattern             |
| _notLike  | NOT_LIKE        | `name_notLike(String)`  | Field does not match the LIKE pattern      |
| _in       | CONTAINS        | `status_in(List)`       | Field value is in the provided collection  |
| _notIn    | NOT_CONTAINS    | `status_notIn(List)`    | Field value is not in the collection       |
| _null     | IS_NULL         | `description_null()`    | Field is null                              |
| _notNull  | NOT_NULL        | `description_notNull()` | Field is not null                          |

## Custom Operations

For more complex queries, you can define custom operations by implementing the `CustomOperation` interface:

1. Create a custom operation that implements the `apply` method
2. Use the `QueryContext` to access the CriteriaBuilder, CriteriaQuery, and Root
3. Build your custom predicate using the JPA Criteria API
4. Add the custom filter to your repository using the `addFilter` method

This allows you to create complex queries that go beyond the standard operations provided by the library.

### Example: Custom Operation with Subquery

```java
public class ProductRepository extends JpaQueryRepository<Product, ProductRepository.Filter> {

    @Override
    protected void buildCriteria() {
        // Standard filters
        addFilter(Filter::id);
        addFilter(Filter::description_like);

        // Custom operation using a subquery
        addFilter(Filter::orderMinimumQuantity_exists, (ctx, value) -> {
            var minimumQuantity = (Integer) value;

            // Create a subquery
            Subquery<Integer> subquery = ctx.criteriaQuery().subquery(Integer.class);
            Root<OrderItem> subRoot = subquery.from(OrderItem.class);

            // Build the subquery condition
            subquery.select(ctx.criteriaBuilder().literal(1))
                .where(ctx.criteriaBuilder().greaterThanOrEqualTo(subRoot.get("quantity"), minimumQuantity),
                    ctx.criteriaBuilder().equal(subRoot.get("product"), ctx.root()));

            // Return an EXISTS predicate
            return ctx.criteriaBuilder().exists(subquery);
        });
    }

    public interface Filter extends JpaQueryRepository.Filter {
        void id(Long id);
        void description_like(String description);
        void orderMinimumQuantity_exists(Integer minimumQuantity);
    }
}

// Usage
List<Product> products = productRepository.query(f -> {
    f.description_like("%keyboard%");
    f.orderMinimumQuantity_exists(5); // Find products ordered with quantity >= 5
}).list();
```

This example demonstrates a custom operation that uses a subquery to find products that have been ordered with a quantity greater than or equal to a specified minimum value.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
