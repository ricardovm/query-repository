# Query Repository

A Java library that simplifies working with JPA Criteria API by providing a fluent, type-safe interface for building and executing JPA queries.

This is still a work in progress, but this release is already usable and provides a good starting point for building JPA repositories with a focus on type safety and ease of use.

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
    <artifactId>query-repository</artifactId>
    <version>0.1.1</version>
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

Extend `BaseQueryRepository` with your entity type and a custom filter interface:

```java
import dev.ricardovm.queryrepository.BaseQueryRepository;

public class OrderRepository extends BaseQueryRepository<Order, OrderRepository.Params> {

    public OrderRepository(EntityManager em) {
        super(em);
    }

    @Override
    protected void buildCriteria() {
        // Define filters
        addFilter(Params::status);
        addFilter(Params::status_in);

        // Define entity fetches
        addEntityFetch(Params::fetchItems);
        addEntityFetch(Params::fetchItemsProduct, "items.product");

        // Define sort fields
        addSortField(Params::sortById);
        addSortField(Params::sortByDate);
        addSortField(Params::sortByTotal_desc);
    }

    @Override
    protected Class<Order> entityClass() {
        return Order.class;
    }

    @Override
    protected Class<Params> paramsClass() {
        return Params.class;
    }

    public interface Params extends BaseQueryRepository.Params {
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
List<Order> orders = orderRepository.query(q -> {
    q.status_in(List.of("SHIPPED", "COMPLETED"));
    q.fetchItems();
    q.fetchItemsProduct();
    q.sortByTotal_desc();
}).list();

// Or get a single result
Optional<Order> order = orderRepository.query(q -> {
    q.status("SHIPPED");
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
public class ProductRepository extends BaseQueryRepository<Product, ProductRepository.Params> {

    @Override
    protected void buildCriteria() {
        // Standard filters
        addFilter(Params::id);
        addFilter(Params::description_like);

        // Custom operation using a subquery
        addFilter(Params::orderMinimumQuantity_exists, (ctx, value) -> {
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

    public interface Params extends BaseQueryRepository.Param****s {
        void id(Long id);
        void description_like(String description);
        void orderMinimumQuantity_exists(Integer minimumQuantity);
    }
}

// Usage
List<Product> products = productRepository.query(q -> {
    q.description_like("%keyboard%");
    q.orderMinimumQuantity_exists(5); // Find products ordered with quantity >= 5
}).list();
```

This example demonstrates a custom operation that uses a subquery to find products that have been ordered with a quantity greater than or equal to a specified minimum value.

## Known Limitations

- It's not possible to use primitive parameters in filter methods. Always use their wrapper classes (e.g., `Integer` instead of `int`, `Long` instead of `long`).
- No support for `@ElementCollection` or `@ManyToMany` relationships yet.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
