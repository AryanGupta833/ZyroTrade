package com.ZyroTrade.ledger;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MySqlMigrationSmokeTests {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("zyrotrade_migrations")
            .withUsername("migration_test")
            .withPassword("migration_test");

    @Test
    void additiveLedgerAndOutboxMigrationsApplyToAnExistingMysqlSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE orders (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY) ENGINE=InnoDB");
            statement.execute("CREATE TABLE daily_pnl_snapshots (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, unrealized_pnl DECIMAL(19,4) NOT NULL, net_pnl DECIMAL(19,4) NOT NULL) ENGINE=InnoDB");
            executeSqlFile(statement, "docs/migrations/mysql-double-entry-ledger.sql");
            executeSqlFile(statement, "docs/migrations/mysql-transactional-outbox.sql");
            executeSqlFile(statement, "docs/migrations/mysql-market-data-snapshot-freshness.sql");

            assertTableExists(connection, "ledger_accounts");
            assertTableExists(connection, "journal_entries");
            assertTableExists(connection, "ledger_lines");
            assertTableExists(connection, "outbox_events");
            assertTableExists(connection, "outbox_inbox_receipts");
            try (ResultSet result = statement.executeQuery("SHOW COLUMNS FROM daily_pnl_snapshots LIKE 'all_quotes_fresh'")) {
                assertThat(result.next()).isTrue();
            }
            try (ResultSet result = statement.executeQuery("SHOW COLUMNS FROM daily_pnl_snapshots LIKE 'all_cost_basis_reviewed'")) {
                assertThat(result.next()).isTrue();
            }
            try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM ledger_accounts WHERE account_code='CASH_ASSET_INR'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM journal_entries")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM outbox_events")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
        }
    }

    private void executeSqlFile(Statement statement, String file) throws Exception {
        String sql = Files.readString(Path.of(file));
        String executable = sql.lines().filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right);
        for (String command : Arrays.stream(executable.split(";")).map(String::trim)
                .filter(part -> !part.isBlank()).toList()) {
            statement.execute(command);
        }
    }

    private void assertTableExists(Connection connection, String table) throws Exception {
        try (ResultSet result = connection.getMetaData().getTables(MYSQL.getDatabaseName(), null, table, null)) {
            assertThat(result.next()).as("table %s is created", table).isTrue();
        }
    }
}
