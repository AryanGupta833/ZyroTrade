# Contributing to ZyroTrade

Thank you for your interest in contributing to ZyroTrade! Contributions that improve reliability, security, testing, documentation, and the trading experience are welcome.

## Getting started

### Prerequisites

- Java 17 or newer
- Maven 3.8+ (Maven 3.9+ recommended)
- Git
- Docker Desktop or Docker Engine with Docker Compose for the full local stack
- A Finnhub API key for live market quotes (optional for startup)

### 1. Fork and clone

Fork the repository on GitHub, then clone your fork:

```bash
git clone https://github.com/<your-username>/ZyroTrade.git
cd ZyroTrade
git remote add upstream https://github.com/AryanGupta833/ZyroTrade.git
```

### 2. Configure the application

Copy the example environment file:

**PowerShell**
```powershell
Copy-Item .env.example .env
```

**Linux/macOS**
```bash
cp .env.example .env
```

Edit `.env` and replace all placeholders. Configure at least:

- `MYSQL_ROOT_PASSWORD`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

The JWT secret must contain at least 32 UTF-8 bytes. Generate a high-entropy value instead of using a sample or placeholder. Set `FINNHUB_API_KEY` if you want live market quotes.

**Never commit `.env`, real credentials, API keys, or JWT secrets.** The `.env.example` file should contain placeholders only.

### 3. Run the application

Start the application and its MySQL database with Docker Compose:

```bash
docker compose up --build
```

The backend should be available at `http://localhost:8080`.

Useful endpoints:

- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The default Compose configuration uses a disposable development database and may drop its schema when the application shuts down cleanly. Do not use this configuration with persistent or production data.

To stop the services:

```bash
docker compose down
```

### 4. Build and test

Build the application:

```bash
mvn -B package -DskipTests
```

Run the test suite:

```bash
mvn test
```

Some integration tests require Docker and a working MySQL/Testcontainers environment. H2 is used for fast tests, while MySQL integration tests verify database-specific behavior such as transaction locking and uniqueness constraints.

If a test fails, inspect its output and check whether it requires Docker or MySQL before assuming the failure is unrelated to your change.

## Project conventions

- **Java:** Use Java 17-compatible language features and the existing Spring Boot patterns.
- **Architecture:** Keep controllers, services, repositories, entities, and DTOs focused on their existing responsibilities.
- **Validation:** Validate user input and return the project's established API error format.
- **Security:** Derive user identity from the authenticated context. Never trust a client-supplied ID as proof of ownership.
- **Database:** Use transactions for related financial operations. Preserve wallet, portfolio, order, and ledger consistency.
- **Money:** Use `BigDecimal` for monetary calculations. Follow existing precision and rounding conventions.
- **Concurrency:** Consider database transactions, locking, uniqueness constraints, and idempotency when modifying orders or balances.
- **External APIs:** Preserve configured timeouts, bounded retries, and failure handling.
- **Secrets:** Never log passwords, JWTs, provider keys, or other sensitive values.
- **Tests:** Add tests for new behavior, validation, authorization, error paths, and relevant database interactions.

Avoid unrelated refactoring in feature or bug-fix pull requests.

## Finding work

Before starting substantial work:

1. Check the open issues for an existing task.
2. Comment on the issue describing your intended approach.
3. Wait for scope or assignment confirmation when requested by the maintainer.
4. Ask questions early if the expected behavior or API contract is unclear.

For new features, describe the problem being solved and discuss the intended behavior before implementing a large change.

## Branch and commit guidelines

Create a focused branch from the latest `master`:

```bash
git fetch upstream
git switch master
git pull upstream master
git switch -c feat/short-description
```

Suggested branch prefixes:

- `feat/` — new features
- `fix/` — bug fixes
- `docs/` — documentation
- `test/` — tests
- `ci/` — continuous integration and automation
- `refactor/` — internal improvements

Use clear commit messages, for example:

```text
feat: add personal stock watchlists
fix: prevent duplicate order execution
test: cover wallet concurrency
docs: improve contributor setup
ci: add backend build workflow
```

## Pull requests

Before opening a pull request:

- [ ] Keep the change focused and explain its motivation.
- [ ] Build the project successfully.
- [ ] Run relevant tests and add tests for new behavior.
- [ ] Check that no secrets, local configuration, or generated files are included.
- [ ] Update documentation when APIs or behavior change.
- [ ] Ensure the GitHub Actions build check passes.

A pull request description should explain:

- What changed and why.
- How the change was implemented.
- How it was tested.
- Any relevant issue links, API changes, database implications, or screenshots.

Do not merge your own contribution unless the repository's maintainers have explicitly authorized it.

## Reporting bugs and requesting features

When reporting a bug, include the steps to reproduce it, expected versus actual behavior, relevant logs with secrets removed, and the environment in which it occurred.

For feature requests, describe the user problem, expected behavior, acceptance criteria, and any relevant compatibility or security concerns.

Never include real passwords, tokens, private user information, or production database contents in an issue.

## Security issues

Do not publish credentials, exploitable vulnerabilities, or sensitive production data in public issues or pull requests. Report security concerns privately to the repository maintainer.

## Community guidelines

Be respectful, constructive, and open to review feedback. Explain technical trade-offs clearly, keep discussions focused, and help make ZyroTrade welcoming to contributors of different experience levels.

Thank you for helping improve ZyroTrade!