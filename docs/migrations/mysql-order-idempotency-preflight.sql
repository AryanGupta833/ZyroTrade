-- Run only if idempotency_key already exists, before creating the unique index.
SELECT idempotency_key, COUNT(*) AS duplicate_count
FROM orders
WHERE idempotency_key IS NOT NULL
GROUP BY idempotency_key
HAVING COUNT(*) > 1;
