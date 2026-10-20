# ZyroTrade backend

ZyroTrade is a Spring Boot 3.2.2 / Java 17 stock trading simulator. It is not a brokerage or a regulated financial service. The application is a modular monolith backed by MySQL; H2 is retained for fast tests.

## Architecture and current behavior

| Area | Responsibility |
| --- | --- |
| `auth`, `security`, `user` | Registration, BCrypt passwords, JWT authentication and role authorization |
| `Orders`, `Wallet`, `Portfolios` | Synchronous BUY/SELL execution, pessimistic wallet lock, cost basis and P&L |
| `ledger` | Immutable posted debit/credit journals and admin wallet reconciliation |
| `outbox` | Transactional trade-completion events, bounded dispatch retries and inbox-deduplicated notifications |
| `Price`, `Stocks`, `PriceAlert` | Finnhub integration, quote freshness, instrument controls and alert evaluation |
| `dailyPnlSnapshot` | Periodically refreshed daily P&L history |
| `api`, `DTO` | Stable error format and frontend response models |

Orders execute synchronously at a fresh, positive quote. Quantity is an integer share count. A successful order response has `status: "EXECUTED"`; there are no pending, failed-order records, limit orders, or cancellation workflow. An order and its wallet debit/credit, holdings/cost-basis change, ledger journal, wallet transaction, and outbox event commit or roll back in the same database transaction. Wallets are locked before portfolio rows are changed. MySQL tests exercise the pessimistic lock and uniqueness behavior at the configured `REPEATABLE-READ` isolation level.

Optional `Idempotency-Key` values are stored with a unique database constraint. A matching retry returns the original order; a different user, stock, side, or quantity with the same key returns `409 IDEMPOTENCY_KEY_CONFLICT`. Calls without a key are independent submissions. Notifications are delivered asynchronously from the committed outbox record.

All supported money is `BigDecimal`; API numbers and monetary columns use four fractional places with HALF_EVEN normalization at quote, execution, and persistence boundaries. Accumulated holding cost basis uses `DECIMAL(29,4)` while prices and wallet currency amounts use `DECIMAL(19,4)`. P&L does not infer an unavailable legacy cost basis from rounded average prices: responses identify `costBasisStatus: "REVIEW_REQUIRED"`, P&L values are null, and trading that holding is blocked until its basis is reviewed and migrated. No fees, withdrawals, or other cash movements are currently modeled.

The cash ledger has unique business references (`WALLET_OPENING:<walletId>`, `MIGRATION_OPENING:<walletId>`, and `ORDER:<orderId>`). Funding debits the cash asset and credits the wallet liability; BUY debits the liability and credits cash; SELL debits cash and credits the liability. Securities and acquisition basis remain in the holdings subledger. The service and JPA callbacks enforce same-currency balance and posted-entry immutability; MySQL constraints enforce positive lines, valid sides, references, foreign keys, and uniqueness. **MySQL does not enforce the aggregate debit-equals-credit sum across journal lines**; that invariant remains application-enforced. Corrections use compensating journals.

The outbox stores `trade.completed.v1` in the order transaction and publishes it to synchronous in-process listeners on a scheduled worker. Delivery is at least once: the notification consumer uses a unique inbox receipt in the same transaction as the notification, so a crash after consuming but before marking the outbox event published does not create duplicate notifications. Failed events back off exponentially, become `FAILED` after eight attempts, and can be inspected/replayed by admins. There is no external Kafka broker or cross-service delivery guarantee.

## Prerequisites and local startup

- Java 17 or newer (the verified build host used Java 21).
- Docker Desktop or Docker Engine with Compose.
- A Finnhub API key for live quotes (optional for boot; without it, prices stay unavailable and orders are rejected as stale/unavailable).

For disposable local development:

```powershell
Copy-Item .env.example .env
# Edit .env. Replace every placeholder, especially JWT_SECRET and both database passwords.
docker compose up --build
```

Compose starts MySQL 8.0.36, waits for its health check, then starts the application on `http://localhost:8080`. The application health endpoint is `GET /actuator/health`; Swagger UI is `/swagger-ui.html`; OpenAPI JSON is `/v3/api-docs`. MySQL is mapped to port 3306. The app uses `JPA_DDL_AUTO=create-drop` by default in this **disposable compose setup** to create an empty development schema; normal application configuration defaults to `validate`. Expect the schema/data to be dropped when the app exits cleanly. Do not use the compose setting against persistent or production data.

Set a new high-entropy JWT key locally; for example, use a cryptographic random generator (`openssl rand -base64 48`) and store the result only in ignored `.env`. `JWT_SECRET` must be present and contain at least 32 UTF-8 bytes. That length check does not establish entropy. `.env.example` contains placeholders only. The repository never needs a committed real database password, JWT, or provider key. The prior source configuration contained a database password and Finnhub key; rotate either if it was ever active or shared. The previous JWT signing key was a source-code placeholder; replacing it invalidates tokens signed with that value.

`CORS_ALLOWED_ORIGINS` is a comma-separated exact-origin allowlist; defaults are local development origins only. Do not set `*` with credentials. Database credentials are required through `DB_PASSWORD`; production `JPA_DDL_AUTO` should remain `validate`.

## Configuration

| Variable | Meaning | Default |
| --- | --- | --- |
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/zyrotrade` |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL user and required password | `zyrotrade` / required |
| `JWT_SECRET` | HS256 signing secret, minimum 32 bytes | required |
| `FINNHUB_API_KEY` | Finnhub quote credential | empty; provider unavailable |
| `CORS_ALLOWED_ORIGINS` | Exact comma-separated browser origins | `http://localhost:3000,http://localhost:8081` |
| `TRADING_MAX_PRICE_AGE_SECONDS` | Maximum age for order quotes and freshness labels | `120` |
| `LEDGER_CURRENCY` | Wallet ledger currency | `INR` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `validate` |
| `FINNHUB_CONNECT_TIMEOUT_MS` / `FINNHUB_READ_TIMEOUT_MS` | Provider timeouts | `2000` / `3000` |
| `FINNHUB_RETRY_ATTEMPTS` | Bounded total attempts, clamped to 1–4 | `2` |
| `FINNHUB_CIRCUIT_FAILURE_THRESHOLD` / `FINNHUB_CIRCUIT_OPEN_SECONDS` | Provider circuit breaker | `5` / `30` |
| `OUTBOX_POLL_DELAY_MS` | Outbox worker fixed delay | `5000` |

## API and frontend contract

All `/api/**` routes require `Authorization: Bearer <JWT>` unless marked public below. `/auth/login`, `/auth/register`, health probes, OpenAPI JSON, and Swagger UI are public. Admin routes require `ROLE_ADMIN`; user identity and ownership always come from the authenticated token. IDs supplied by a client never grant access to another user's records. Responses use numeric JSON values for money, four decimal places, and ISO-8601 timestamps with offsets (P&L snapshot dates are ISO calendar dates).

| Method and path | Access | Contract / behavior |
| --- | --- | --- |
| `POST /auth/register` | Public | JSON `{"username":"aryan","password":"replace-with-long-password"}`; username 3–64 chars, password 12–72 chars and at most 72 UTF-8 bytes. Creates a wallet with the simulator's initial INR funding and its opening journal. |
| `POST /auth/login` | Public | JSON username/password; returns `{"token":"..."}`. Token lifetime is one hour. |
| `GET /api/me` | User | Authenticated user id, username and role. |
| `GET /api/stocks` | User | Supported symbols, numeric prices, `quoteStatus` (`FRESH`, `STALE`, `UNAVAILABLE`), provider timestamp and max age. |
| `GET /api/stocks/search?q=apple&page=0&size=20` | User | Case-insensitive symbol/company search; zero-based page, size capped at 100. |
| `GET /api/wallet` | User | Current user's balance, configured currency and update time. |
| `GET /api/wallet/transactions` | User | Current user's transaction history. Use `/api/wallet/transactions/page?page=0&size=20` for bounded pages. |
| `POST /api/orders?stockId=1&type=BUY&quantity=2` | User | Executes synchronously. Optional `Idempotency-Key` header up to 128 characters. Returns `OrderResponse`. |
| `GET /api/orders` | User | Current user's order history. |
| `GET /api/orders/page?page=0&size=20` | User | Newest-first page, size capped at 100. |
| `GET /api/orders/{id}` | Owner | Order detail; another user's ID returns 404. |
| `GET /api/portfolio` | User | Current holdings, average acquisition price and cost basis. |
| `GET /api/portfolio/pnl` | User | Holding values, P&L, quote status and cost-basis status. P&L is null if basis needs review. |
| `GET /api/pnl/unrealized` | User | Per-symbol unrealized P&L with quote and cost-basis status. |
| `GET /api/pnl/history?days=30` | User | Daily snapshot history, 1–365 days. |
| `GET /api/pnl/history/range?from=2026-10-01&to=2026-10-20` | User | Inclusive ISO dates; ordered range maximum 366 days. |
| `GET /api/performance/history` | User | Snapshot history from the legacy-compatible route. |
| `POST /api/alerts` | User | JSON `{"symbol":"AAPL","targetPrice":250.0000,"type":"ABOVE"}`; creates an alert for an existing symbol. |
| `GET /api/alerts`, `DELETE /api/alerts/{id}` | Owner | List/delete own alerts; foreign IDs return 404. |
| `GET /api/notifications`, `PUT /api/notifications/{id}/read` | Owner | List and mark own notifications read; foreign IDs return 404. |
| `POST /api/admin/stocks`, `PUT /api/admin/stocks/{id}/halt`, `PUT /api/admin/stocks/{id}/resume`, `DELETE /api/admin/stocks/{id}` | Admin | Instrument controls. Resume requires a valid fresh quote. |
| `GET /api/admin/ledger/wallet-reconciliation` | Admin | Reports wallet/ledger discrepancies, opening-entry presence, expected order postings, and missing order journal IDs; read-only and safe to repeat. |
| `GET /api/admin/outbox`, `POST /api/admin/outbox/{id}/replay` | Admin | Inspect event status and replay only failed events. |
| `GET /actuator/health` | Public | Liveness/readiness health with details hidden. |
| `GET /actuator/prometheus` | Admin | Prometheus scrape endpoint. |

`PageResponse` uses `{content,page,size,totalElements,totalPages,first,last}`. Common errors use `{timestamp,status,code,message,details}`. Important codes include `VALIDATION_ERROR`, `UNAUTHENTICATED`, `INVALID_TOKEN`, `FORBIDDEN`, `INSUFFICIENT_FUNDS`, `INSUFFICIENT_HOLDINGS`, `IDEMPOTENCY_KEY_CONFLICT`, `COST_BASIS_REVIEW_REQUIRED`, `DATA_CONFLICT`, and `INTERNAL_ERROR`. Internal exception text, SQL errors and stack traces are not returned.

The machine-readable contract includes schemas and common errors: `GET /v3/api-docs`; interactive docs: `/swagger-ui.html`. Idempotency, status semantics, pagination, DTOs, and validation are described in the generated document and this endpoint inventory.

Example authentication and order request:

```sh
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"aryan","password":"replace-with-long-password"}'

curl -X POST 'http://localhost:8080/api/orders?stockId=1&type=BUY&quantity=2' \
  -H 'Authorization: Bearer <token>' \
  -H 'Idempotency-Key: ui-order-7c3451f0' \
  -H 'Accept: application/json'
```

## Quote reliability and observability

Finnhub uses explicit connect/read timeouts, bounded retries with exponential delay (honoring numeric `Retry-After` up to two seconds), and a small process-local circuit breaker. The scheduled job refreshes supported stocks every minute, updates only with a newer provider timestamp, and records provider time rather than refresh time. Invalid, future, expired and unavailable quotes are not marked fresh; no stale fallback is presented as fresh, and order submission rechecks quote age after acquiring its wallet lock. Provider refresh never resets an admin trading halt. Price alerts also require fresh quotes. There is no Redis cache or cross-instance request coalescing. Each instance can still make duplicate provider requests, though row locking prevents an older quote from overwriting a newer one. Provider limits/outages can leave trading unavailable until a fresh price arrives.

Spring Boot Actuator and Micrometer expose `http.server.requests`, datasource pool metrics, JVM metrics, `orders.submission`, `orders.submission.duration`, and `market_data.refresh` metrics with low-cardinality labels. `/actuator/health` hides details; Prometheus is role-protected. Logs must not include credentials, JWTs, or financial payloads.

## Database migrations and existing data

No production database was inspected or modified. This repository does not run Flyway/Liquibase; `JPA_DDL_AUTO=validate` is the normal runtime default. SQL in `docs/migrations/` is operator-reviewed, manual MySQL SQL and must be rehearsed against a restored copy of the actual schema. MySQL DDL may commit implicitly. Table and column names may differ from historical deployments, so confirm them first.

Suggested controlled sequence:

1. Stop writes and create a restorable backup with routines/triggers, then restore it to a staging MySQL instance. For example, use `mysqldump --single-transaction --routines --triggers --user="$DB_USERNAME" --password "$SOURCE_DATABASE" > backup.sql`; enter the password at the prompt. Restore a staging copy with `mysql --host="$STAGING_DB_HOST" --user="$DB_USERNAME" --password --database="$STAGING_DATABASE" < backup.sql`. Preserve the exact preflight results and backup outside the application host.
2. Review `mysql-order-idempotency.sql`. If the column is absent, apply its nullable-column/index DDL (existing rows remain NULL). If it already exists, run `mysql-order-idempotency-preflight.sql` and create the unique index only when there are no duplicate non-NULL keys. Confirm the index in `SHOW INDEX`.
3. Run and export the read-only `mysql-decimal-financial-preflight.sql` results. Reconcile every rounding delta, NULL, range overflow, and relationship. Only then apply `mysql-decimal-financial-columns.sql` to a restored copy and production during an approved window.
4. The decimal DDL adds nullable cost-basis columns without backfilling from rounded average prices. Build/review basis from an approved source such as verified acquisition/order history, compare resulting quantity and basis to the holdings reconciliation, then run `mysql-cost-basis-validation.sql`. Use `mysql-cost-basis-finalize.sql` only after both NULL counts are zero. If no reliable source exists, keep those holdings blocked and basis status marked for review; do not invent historical entries.
5. Apply `mysql-double-entry-ledger.sql`, then `mysql-transactional-outbox.sql`, then `mysql-market-data-snapshot-freshness.sql`. These add structures and metadata; they do not fabricate historical order postings or change wallet balances. The opening journal seed account is safe to repeat, but the `CREATE TABLE` scripts themselves are one-time migrations.
6. Pause execution for a reviewed wallet cutover. Reconcile every stored balance to approved records and post exactly one `MIGRATION_OPENING:<walletId>` per existing wallet through `LedgerService.postOpeningWalletBalance`. Repeating the same reference and amount returns the existing entry; a changed amount is rejected. The service does not update the wallet balance. Resume only after reconciliation passes.
7. Run the application with `JPA_DDL_AUTO=validate`; compare row counts, wallet balances, ledger totals, holdings/cost basis, P&L snapshots, and sample JSON against the backup. Keep backup and restore instructions until owners sign off. If validation or reconciliation fails, stop the rollout and restore the verified backup rather than manually editing ledger history.

The migration scripts are not applied by tests against any real database. MySQL smoke tests execute the additive ledger, outbox and P&L metadata scripts against an isolated disposable MySQL container. That does not validate an operator's historical production schema or the destructive/precision-changing decimal migration.

## Tests and build

The project targets Java 17 and the Maven wrapper. On this Windows host, Maven's resolved home points at `C:\`, and default cache/output paths were not writable/reliable. Keep Maven's repository and compiler output local using:

```powershell
$env:TEMP='C:\Users\Aryan Gupta Aspire 7\.codex\visualizations\2026\10\20\01a158ac-342e-7291-8dfa-b338085218f9'
.\mvnw.cmd "-Dmaven.repo.local=$env:TEMP\zyrotrade-m2" "-Dbuild.output.dir=$env:TEMP\zyrotrade-build" test
```

H2 covers fast order, money, ledger, outbox, market-data, validation and API security tests. MySQL Testcontainers uses MySQL 8.0.36 and Docker; a test skipped because Docker is unavailable is not MySQL verification. On the current Docker Desktop named-pipe/API combination, the verified full command was:

```powershell
$env:TEMP='C:\Users\Aryan Gupta Aspire 7\.codex\visualizations\2026\10\20\01a158ac-342e-7291-8dfa-b338085218f9'
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'
$env:DOCKER_API_VERSION='1.44'
$env:TESTCONTAINERS_DOCKER_CLIENT_STRATEGY='org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy'
.\mvnw.cmd "-Dmaven.repo.local=$env:TEMP\zyrotrade-m2" "-Dbuild.output.dir=$env:TEMP\zyrotrade-build" "-Dapi.version=1.44" package
```

Verification on 2026-10-20: the full `package` run passed 45 tests (0 failures/errors/skips), including MySQL 8.0.36 Testcontainers tests. Separately, the opt-in MySQL benchmark passed 1 test with 0 failures. `git diff --check` completed cleanly. Container tests cover disposable MySQL only; migration DDL was smoke-tested on an isolated schema and no existing database was changed. `mvn dependency:analyze` is available for the current dependency graph.

## Performance benchmark

No latency or throughput improvement is claimed. See [docs/performance/order-benchmark.md](docs/performance/order-benchmark.md) for the benchmark harness, exact MySQL dataset/workload, warm-up, measurement duration, and measured percentiles. Measurements are specific to the stated host and disposable Testcontainers MySQL; they are not a production service-level objective.

## Known limits and frontend handoff

- Signup seeds the simulator wallet with INR 10,000; no top-up/withdrawal endpoint exists. Fees are not modeled.
- Existing wallets require an audited opening-ledger cutover before executing orders. Legacy holdings with unreviewed cost basis are reported as such and cannot be bought into or sold until reviewed.
- The cash ledger is not an audited brokerage ledger: the database does not enforce aggregate line balancing, and securities remain in the holdings subledger.
- Orders are synchronous market orders only. There is no cancellation state, pending execution queue, Kafka, Redis, or external outbox transport.
- Provider rate limits and network outages may make quotes stale. No price is fabricated; trading rejects stale data.
- P&L snapshots are refreshed periodically into one row per user/day and include freshness/basis flags. Historical source quote and basis quality cannot be reconstructed if it was not recorded.
- MySQL locking and migration smoke tests cover disposable MySQL 8.0.36, not the exact production schema, topology, or failover configuration.

Frontend handoff checklist:

1. Copy `.env.example` to local-only `.env`, set a new JWT/database credential and exact frontend origin, then run `docker compose up --build`.
2. Register or log in; store the returned bearer token securely in the frontend's chosen session strategy and send it in `Authorization` on protected routes.
3. Use `/api/stocks` freshness fields before enabling an order button. Treat `STALE`/`UNAVAILABLE` as non-tradable and show the error code returned by the API.
4. Submit integer quantities with a stable per-user-action `Idempotency-Key`; reuse the same key only for retries of the same stock, side, and quantity.
5. Consume `OrderResponse`, wallet balance/transactions, holdings and P&L DTOs. Respect `quoteStatus`, nullable P&L and `costBasisStatus`; never interpret null as zero.
6. Use `/page` and `/search` endpoints for bounded result sets. Handle 401/403/404/409 using stable `code`, not message parsing.
7. Confirm browser CORS origin matches `CORS_ALLOWED_ORIGINS`; use `/swagger-ui.html` or `/v3/api-docs` as the authoritative generated contract.
8. Admin-only ledger reconciliation, outbox inspection/replay, Prometheus metrics, and instrument controls require a token with `ROLE_ADMIN`.

The backend remains a simulator and should not be described as production brokerage infrastructure or as regulatorily compliant.
