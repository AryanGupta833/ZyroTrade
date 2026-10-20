-- Additive transactional outbox and idempotent local consumer inbox for MySQL 8.0.36+.
-- Take and verify a backup first. This script does not modify existing financial rows.

CREATE TABLE outbox_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_uuid CHAR(36) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  payload LONGTEXT NOT NULL,
  status VARCHAR(12) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  published_at DATETIME(6) NULL,
  last_error VARCHAR(160) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_outbox_event_uuid (event_uuid),
  UNIQUE KEY uq_outbox_type_aggregate (event_type, aggregate_id),
  KEY ix_outbox_due (status, next_attempt_at, id),
  CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
  CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0),
  CONSTRAINT fk_outbox_order FOREIGN KEY (aggregate_id) REFERENCES orders(id)
) ENGINE=InnoDB;

CREATE TABLE outbox_inbox_receipts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_uuid CHAR(36) NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_outbox_inbox_event_uuid (event_uuid)
) ENGINE=InnoDB;

-- Verify the required index/constraints and use JPA_DDL_AUTO=validate before rollout.
-- Existing orders are not backfilled: replaying historic trade side effects would be unsafe.
-- The outbox publisher emits trade.completed.v1 events created only for new committed orders.
