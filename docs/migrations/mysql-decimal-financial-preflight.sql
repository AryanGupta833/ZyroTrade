-- Read-only preflight. Export and review every returned row before approving the DDL file.
-- DECIMAL(19,4) supports 15 integer digits; DECIMAL(29,4) cost basis supports 25 integer digits.
SELECT id, balance, ROUND(balance, 4) proposed, balance - ROUND(balance, 4) delta FROM wallets WHERE balance <> ROUND(balance, 4);
SELECT id, amount, ROUND(amount, 4) proposed, amount - ROUND(amount, 4) delta FROM wallet_transaction WHERE amount <> ROUND(amount, 4);
SELECT id, balance_after, ROUND(balance_after, 4) proposed, balance_after - ROUND(balance_after, 4) delta FROM wallet_transaction WHERE balance_after <> ROUND(balance_after, 4);
SELECT id, price, ROUND(price, 4) proposed, price - ROUND(price, 4) delta FROM orders WHERE price <> ROUND(price, 4);
SELECT id, realized_pnl, ROUND(realized_pnl, 4) proposed, realized_pnl - ROUND(realized_pnl, 4) delta FROM orders WHERE realized_pnl IS NOT NULL AND realized_pnl <> ROUND(realized_pnl, 4);
SELECT id, price, ROUND(price, 4) proposed, price - ROUND(price, 4) delta FROM stocks WHERE price <> ROUND(price, 4);
SELECT id, avg_price, ROUND(avg_price, 4) proposed, avg_price - ROUND(avg_price, 4) delta FROM portfolio WHERE avg_price <> ROUND(avg_price, 4);
SELECT id, avg_price, quantity, ROUND(avg_price * quantity, 4) illustrative_only FROM portfolio;
SELECT id, avg_buy_price, ROUND(avg_buy_price, 4) proposed, avg_buy_price - ROUND(avg_buy_price, 4) delta FROM portfolio_holdings WHERE avg_buy_price <> ROUND(avg_buy_price, 4);
SELECT id, avg_buy_price, quantity, ROUND(avg_buy_price * quantity, 4) illustrative_only FROM portfolio_holdings;
SELECT id, target_price, ROUND(target_price, 4) proposed, target_price - ROUND(target_price, 4) delta FROM price_alerts WHERE target_price <> ROUND(target_price, 4);
SELECT id, realized_pnl, ROUND(realized_pnl, 4) proposed, realized_pnl - ROUND(realized_pnl, 4) delta FROM daily_pnl_snapshots WHERE realized_pnl <> ROUND(realized_pnl, 4);
SELECT id, unrealized_pnl, ROUND(unrealized_pnl, 4) proposed, unrealized_pnl - ROUND(unrealized_pnl, 4) delta FROM daily_pnl_snapshots WHERE unrealized_pnl <> ROUND(unrealized_pnl, 4);
SELECT id, net_pnl, ROUND(net_pnl, 4) proposed, net_pnl - ROUND(net_pnl, 4) delta FROM daily_pnl_snapshots WHERE net_pnl <> ROUND(net_pnl, 4);
SELECT id, wallet_balance, ROUND(wallet_balance, 4) proposed, wallet_balance - ROUND(wallet_balance, 4) delta FROM daily_pnl_snapshots WHERE wallet_balance <> ROUND(wallet_balance, 4);
SELECT id, portfolio_value, ROUND(portfolio_value, 4) proposed, portfolio_value - ROUND(portfolio_value, 4) delta FROM daily_pnl_snapshots WHERE portfolio_value <> ROUND(portfolio_value, 4);

-- DECIMAL range checks: query every affected numeric field for absolute values >= 1000000000000000.
-- Count and export NULLs in fields that will be NOT NULL, duplicate/invalid ID links, and values where
-- trade-derived cost basis disagrees with currently stored holding quantity before approving any change.
SELECT table_name, row_id, column_name, value
FROM (
  SELECT 'wallets' AS table_name, id AS row_id, 'balance' AS column_name, balance AS value FROM wallets WHERE ABS(balance) >= 1000000000000000
  UNION ALL SELECT 'wallet_transaction', id, 'amount', amount FROM wallet_transaction WHERE ABS(amount) >= 1000000000000000
  UNION ALL SELECT 'wallet_transaction', id, 'balance_after', balance_after FROM wallet_transaction WHERE ABS(balance_after) >= 1000000000000000
  UNION ALL SELECT 'orders', id, 'price', price FROM orders WHERE ABS(price) >= 1000000000000000
  UNION ALL SELECT 'orders', id, 'realized_pnl', realized_pnl FROM orders WHERE ABS(realized_pnl) >= 1000000000000000
  UNION ALL SELECT 'stocks', id, 'price', price FROM stocks WHERE ABS(price) >= 1000000000000000
  UNION ALL SELECT 'portfolio', id, 'avg_price', avg_price FROM portfolio WHERE ABS(avg_price) >= 1000000000000000
  UNION ALL SELECT 'portfolio_holdings', id, 'avg_buy_price', avg_buy_price FROM portfolio_holdings WHERE ABS(avg_buy_price) >= 1000000000000000
  UNION ALL SELECT 'price_alerts', id, 'target_price', target_price FROM price_alerts WHERE ABS(target_price) >= 1000000000000000
  UNION ALL SELECT 'daily_pnl_snapshots', id, 'realized_pnl', realized_pnl FROM daily_pnl_snapshots WHERE ABS(realized_pnl) >= 1000000000000000
  UNION ALL SELECT 'daily_pnl_snapshots', id, 'unrealized_pnl', unrealized_pnl FROM daily_pnl_snapshots WHERE ABS(unrealized_pnl) >= 1000000000000000
  UNION ALL SELECT 'daily_pnl_snapshots', id, 'net_pnl', net_pnl FROM daily_pnl_snapshots WHERE ABS(net_pnl) >= 1000000000000000
  UNION ALL SELECT 'daily_pnl_snapshots', id, 'wallet_balance', wallet_balance FROM daily_pnl_snapshots WHERE ABS(wallet_balance) >= 1000000000000000
  UNION ALL SELECT 'daily_pnl_snapshots', id, 'portfolio_value', portfolio_value FROM daily_pnl_snapshots WHERE ABS(portfolio_value) >= 1000000000000000
) AS out_of_range;
