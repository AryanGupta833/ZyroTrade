-- One-time DDL only. Confirm the idempotency_key column does not already exist before running.
-- If it exists, use mysql-order-idempotency-preflight.sql and add only the missing unique index after review.
ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128) NULL;
CREATE UNIQUE INDEX uq_orders_idempotency_key ON orders (idempotency_key);

-- Post-check: SHOW INDEX FROM orders; confirm multiple NULLs are permitted and non-NULL keys are unique.
