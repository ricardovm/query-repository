INSERT INTO products (id, name, description, price) VALUES (1, 'Laptop', 'High-performance laptop', 1299.99);
INSERT INTO products (id, name, description, price) VALUES (2, 'Smartphone', 'Latest smartphone model', 799.99);
INSERT INTO products (id, name, description, price) VALUES (3, 'Headphones', 'Noise-cancelling headphones', 199.99);
INSERT INTO products (id, name, description, price) VALUES (4, 'Tablet', '10-inch tablet', 499.99);
INSERT INTO products (id, name, description, price) VALUES (5, 'Monitor', '27-inch 4K monitor', 349.99);

INSERT INTO orders (id, customerName, orderDate, status) VALUES (1, 'John Doe', '2023-01-15 10:30:00', 'COMPLETED');
INSERT INTO orders (id, customerName, orderDate, status) VALUES (2, 'Jane Smith', '2023-02-20 14:45:00', 'PROCESSING');
INSERT INTO orders (id, customerName, orderDate, status) VALUES (3, 'Bob Johnson', '2023-03-05 09:15:00', 'SHIPPED');

INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (1, 1, 1, 1, 1299.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (2, 1, 3, 1, 199.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (3, 2, 2, 2, 799.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (4, 2, 4, 1, 499.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (5, 3, 5, 2, 349.99);
INSERT INTO order_items (id, order_id, product_id, quantity, unitPrice) VALUES (6, 3, 3, 1, 199.99);
