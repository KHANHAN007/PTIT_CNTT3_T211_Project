# Rikkei Bank REST API

Production-oriented modular monolith for the Rikkei Bank SRS. The system uses Spring Boot, MySQL, Redis, JWT, Flyway, AOP audit logging, double-entry ledger, eKYC workflow, fraud rules, maker-checker approval and interbank simulation.

## Architecture

```text
Client -> Spring Security/JWT -> Controller -> Service -> Repository -> MySQL
                              -> Redis (blacklist, OTP, rate limit, reset token)
                              -> Storage adapter (eKYC documents)
                              -> Mail gateway
```

Financial writes run inside database transactions. Source and target accounts are locked in stable ID order before balances change. Every completed transfer writes matching debit and credit ledger entries.

## Run

Requirements: Docker Desktop with Docker Compose.

```bash
docker compose up --build
```

Services:

- API: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Health: `http://localhost:8081/actuator/health`
- Prometheus metrics: `http://localhost:8081/actuator/prometheus`
- MailHog: `http://localhost:8025`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- MySQL: `localhost:3307`
- Redis: `localhost:6379`

Development seed accounts:

- Admin: `admin` / `Admin@123456`
- Staff: `staff` / `Staff@123456`

Change these passwords and all secrets before using a non-local environment.

## Main API Groups

- `/api/auth/**`: login, refresh rotation, Redis-backed logout, password reset
- `/api/v1/public/**`: customer registration and registration with eKYC
- `/api/v1/admin/**`: user and account administration
- `/api/v1/staff/**`: eKYC decisions and maker-checker transfer approval
- `/api/v1/customer/**`: balances, PIN, eKYC, OTP, transfer and statements

Transfer requests require an OTP challenge and an idempotency key. Transfers at or above the configured approval threshold, or transfers with high fraud scores, enter `PENDING_APPROVAL`.

For simulated interbank behavior, use external bank code `TIMEOUT` to produce a retryable processing transfer or `REJECT` to produce a failed transfer.

## Security

- Passwords and transaction PINs use BCrypt.
- Access tokens are short-lived JWTs.
- Refresh tokens are opaque AES-256-GCM encrypted values, rotate on use, and are stored only as SHA-256 hashes.
- Reuse of an old refresh token revokes its token family.
- Logout stores the access token in Redis with an expiration TTL.
- CCCD/passport identifiers are encrypted with AES-GCM before persistence.
- Public registration cannot assign privileged roles.
- Correlation IDs and immutable audit records support traceability.

## Tests

```bash
./gradlew test
```

The suite contains service and controller unit tests. The concurrent transfer integration test uses Testcontainers with real MySQL and Redis and automatically skips when Docker is unavailable.

## Configuration

Use environment variables from `.env.example`. Flyway migrations are enabled in Docker with Hibernate schema validation. Local direct execution defaults to schema update so code can be developed before infrastructure is started.
