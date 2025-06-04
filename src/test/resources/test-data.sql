INSERT INTO customers (id, name) VALUES (1, 'John Doe');
INSERT INTO customers (id, name) VALUES (2, 'Jane Smith');
INSERT INTO customers (id, name) VALUES (3, 'Bob Johnson');

INSERT INTO categories (id, name) VALUES (1, 'Electronics');
INSERT INTO categories (id, name) VALUES (2, 'Accessories');
INSERT INTO categories (id, name) VALUES (3, 'Tablets');

INSERT INTO products (id, name, description, price, category_id) VALUES (1, 'Laptop', 'High-performance laptop', 1299.99, 1);
INSERT INTO products (id, name, description, price, category_id) VALUES (2, 'Smartphone', 'Latest smartphone model', 799.99, 1);
INSERT INTO products (id, name, description, price, category_id) VALUES (3, 'Headphones', 'Noise-cancelling headphones', 199.99, 2);
INSERT INTO products (id, name, description, price, category_id) VALUES (4, 'Tablet', '10-inch tablet', 499.99, 3);
INSERT INTO products (id, name, description, price, category_id) VALUES (5, 'Monitor', '27-inch 4K monitor', 349.99, 1);

INSERT INTO orders (id, customer_id, order_date, status) VALUES (1, 1, '2023-01-15 10:30:00', 'COMPLETED');
INSERT INTO orders (id, customer_id, order_date, status) VALUES (2, 2, '2023-02-20 14:45:00', 'PROCESSING');
INSERT INTO orders (id, customer_id, order_date, status) VALUES (3, 3, '2023-03-05 09:15:00', 'SHIPPED');

INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (1, 1, 1, 1, 1299.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (2, 1, 3, 1, 199.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (3, 2, 2, 2, 799.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (4, 2, 4, 1, 499.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (5, 3, 5, 2, 349.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (6, 3, 3, 1, 199.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES (7, 3, 2, 12, 199.99);
