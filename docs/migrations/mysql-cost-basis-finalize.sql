-- Run only after mysql-cost-basis-validation.sql returns zero for both missing counts and an
-- independent review confirms every populated basis agrees with the approved acquisition source.
ALTER TABLE portfolio MODIFY cost_basis DECIMAL(29,4) NOT NULL;
ALTER TABLE portfolio_holdings MODIFY cost_basis DECIMAL(29,4) NOT NULL;
