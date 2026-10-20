-- Review-only ledger schema migration for MySQL 8.0.36+.
-- This project has no migration runner. Back up first and apply in an approved maintenance window.
-- The DDL is additive; it does not alter wallets, wallet transactions, orders, or holdings.
-- DO NOT synthesize entries for existing balances in this script.

CREATE TABLE ledger_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_code VARCHAR(96) NOT NULL,
  name VARCHAR(160) NOT NULL,
  account_type VARCHAR(16) NOT NULL,
  currency CHAR(3) NOT NULL,
  wallet_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ledger_account_code (account_code),
  UNIQUE KEY uq_wallet_ledger_currency (wallet_id, currency),
  KEY ix_ledger_account_wallet (wallet_id),
  CONSTRAINT chk_ledger_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'))
) ENGINE=InnoDB;

CREATE TABLE journal_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  business_reference VARCHAR(128) NOT NULL,
  description VARCHAR(255) NOT NULL,
  currency CHAR(3) NOT NULL,
  status VARCHAR(12) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  reversal_of_id BIGINT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_journal_business_reference (business_reference),
  UNIQUE KEY uq_journal_reversal_of (reversal_of_id),
  CONSTRAINT fk_journal_reversal FOREIGN KEY (reversal_of_id) REFERENCES journal_entries(id),
  KEY ix_journal_created_at (created_at),
  CONSTRAINT chk_journal_status CHECK (status = 'POSTED')
) ENGINE=InnoDB;

CREATE TABLE ledger_lines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  journal_entry_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  line_number INT NOT NULL,
  side VARCHAR(8) NOT NULL,
  amount DECIMAL(19,4) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_ledger_entry_line_number (journal_entry_id, line_number),
  KEY ix_ledger_line_account (account_id),
  KEY ix_ledger_line_journal (journal_entry_id),
  CONSTRAINT fk_ledger_line_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id),
  CONSTRAINT fk_ledger_line_account FOREIGN KEY (account_id) REFERENCES ledger_accounts(id),
  CONSTRAINT chk_ledger_line_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_ledger_line_side CHECK (side IN ('DEBIT', 'CREDIT'))
) ENGINE=InnoDB;

-- Required chart account for the current INR-only simulator. Safe to rerun.
INSERT INTO ledger_accounts (account_code, name, account_type, currency, wallet_id)
VALUES ('CASH_ASSET_INR', 'Cash settlement asset (INR)', 'ASSET', 'INR', NULL)
ON DUPLICATE KEY UPDATE account_code = VALUES(account_code);

-- Validation: run JPA_DDL_AUTO=validate after DDL; verify all three tables, indexes, constraints,
-- and the seeded account before deploying the application.
-- Existing wallets need an audited cutover opening journal per wallet, reference MIGRATION_OPENING:<walletId>.
-- Determine approved amounts from reconciled wallet rows and the transaction history as of the cutover time.
-- Block order execution until every wallet has a reviewed opening posting; reconciliation reports missing balances.
-- Never create or backdate historical trade postings automatically. Keep pre-migration statements and approval evidence.
