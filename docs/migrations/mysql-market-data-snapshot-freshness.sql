-- Additive metadata migration for snapshots recorded before stale-quote visibility was added.
-- Existing rows are marked false (unknown), not retroactively treated as fresh.
ALTER TABLE daily_pnl_snapshots
  ADD COLUMN all_quotes_fresh BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN all_cost_basis_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
  MODIFY unrealized_pnl DECIMAL(19,4) NULL,
  MODIFY net_pnl DECIMAL(19,4) NULL;

-- Verify both flags and NULL-capable P&L columns. Existing rows retain their numbers but are marked
-- unknown/false; do not infer historical quote freshness or cost-basis validity.
