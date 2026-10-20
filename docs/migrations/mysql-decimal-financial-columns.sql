-- APPLY ONLY AFTER reviewing mysql-decimal-financial-preflight.sql and rehearsing on a restored copy.
-- MySQL DDL implicitly commits. This is an operator-run migration, not a repeatable migration runner.
-- Confirm every table/column name against the target schema and confirm backups before execution.
-- The cost-basis columns are deliberately nullable: populate them only from reviewed acquisition/order records.
-- Do not infer historical cost basis from the rounded average-price fields.

ALTER TABLE wallets MODIFY balance DECIMAL(19,4) NOT NULL;
ALTER TABLE wallet_transaction
  MODIFY amount DECIMAL(19,4) NOT NULL,
  MODIFY balance_after DECIMAL(19,4) NOT NULL;
ALTER TABLE orders
  MODIFY price DECIMAL(19,4) NOT NULL,
  MODIFY realized_pnl DECIMAL(19,4) NULL;

ALTER TABLE portfolio ADD COLUMN cost_basis DECIMAL(29,4) NULL;
ALTER TABLE portfolio_holdings ADD COLUMN cost_basis DECIMAL(29,4) NULL;

ALTER TABLE stocks MODIFY price DECIMAL(19,4) NOT NULL;
ALTER TABLE portfolio MODIFY avg_price DECIMAL(19,4) NOT NULL;
ALTER TABLE portfolio_holdings MODIFY avg_buy_price DECIMAL(19,4) NOT NULL;
ALTER TABLE price_alerts MODIFY target_price DECIMAL(19,4) NOT NULL;
ALTER TABLE daily_pnl_snapshots
  MODIFY realized_pnl DECIMAL(19,4) NOT NULL,
  MODIFY unrealized_pnl DECIMAL(19,4) NOT NULL,
  MODIFY net_pnl DECIMAL(19,4) NOT NULL,
  MODIFY wallet_balance DECIMAL(19,4) NOT NULL,
  MODIFY portfolio_value DECIMAL(19,4) NOT NULL;

-- STOP before enabling this application version until a reviewed source populates cost_basis in both tables.
-- Verify no NULL cost basis rows, compare quantities and balances to the approved portfolio/order reconciliation,
-- then apply the separately documented NOT NULL finalization step.
