-- Read-only checks. Both NULL counts must be zero after the reviewed basis backfill.
SELECT COUNT(*) AS missing_portfolio_cost_basis FROM portfolio WHERE cost_basis IS NULL;
SELECT COUNT(*) AS missing_portfolio_holdings_cost_basis FROM portfolio_holdings WHERE cost_basis IS NULL;

-- Separately reconcile non-NULL cost basis and quantities against the approved acquisition/order source.
