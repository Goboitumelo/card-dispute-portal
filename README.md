# Card Dispute Portal

Backend service that lets bank customers raise and track disputes on card transactions. Built with Kotlin, Spring Boot 4, PostgreSQL 16, Flyway, and Spring Security (HTTP Basic).

## Requirements

- JDK 21 (the Gradle toolchain downloads one if needed)
- Docker Desktop, for Docker Compose and the Testcontainers test suite

## Build

```bash
./gradlew bootJar
```

The runnable jar is written to `build/libs/`.

## Run with Docker Compose

```bash
docker compose up --build -d
```

The API is available at `http://localhost:8080`. PostgreSQL is published on `127.0.0.1:5432` for local tools.

| Variable | Default | Purpose |
| --- | --- | --- |
| `POSTGRES_PASSWORD` | `dispute` | Database password, shared by `db` and `api`. |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` loads the demo seed data. |

Stop the stack with `docker compose down`. Add `-v` to also delete the database volume.

## Run locally without the API container

Start only the database, then run the app against it:

```bash
docker compose up -d db

SPRING_PROFILES_ACTIVE=dev \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/dispute \
SPRING_DATASOURCE_USERNAME=dispute \
SPRING_DATASOURCE_PASSWORD=dispute \
./gradlew bootRun
```

Flyway applies the migrations on startup. Hibernate runs with `ddl-auto=validate`, so the schema is never changed by the application.

## Test

```bash
./gradlew test
```

Docker must be running. The integration tests start `postgres:16` with Testcontainers. Results are written to `build/test-results/test/`.

## API

All endpoints require HTTP Basic authentication with a customer's email and password, except `/actuator/health`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/actuator/health` | Health check (public). |
| `GET` | `/api/transactions` | The caller's card transactions across all cards. |
| `POST` | `/api/disputes` | Submit a dispute. Returns `201` with a `Location` header. |
| `GET` | `/api/disputes` | The caller's disputes. |
| `GET` | `/api/disputes/{id}` | One dispute. |
| `GET` | `/api/disputes/{id}/history` | Status history for one dispute. |
| `POST` | `/api/disputes/{id}/withdraw` | Withdraw an open dispute. Optional body: `{"note": "..."}`. |

Submit a dispute:

```bash
curl -u thandi@example.com:password \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"b0000000-0000-0000-0000-000000000002","reason":"DUPLICATE_CHARGE","description":"Charged twice","disputedAmountMinor":129900}' \
  http://localhost:8080/api/disputes
```

Error responses use `application/problem+json` (RFC 9457):

- `404` for a missing dispute, or one owned by another customer.
- `409` for a second open dispute on the same transaction, a withdrawal of a closed dispute, or a concurrent update.
- `422` for a dispute outside the window or above the transaction amount.
- `400` for invalid input.

## Dispute rules

- A dispute must be raised within 120 days of the transaction's date.
- The disputed amount must be positive and no more than the transaction amount.
- A transaction can have only one open dispute at a time.
- Status flow: `SUBMITTED` to `UNDER_REVIEW`, then `INFO_REQUESTED`, `RESOLVED_ACCEPTED`, `RESOLVED_REJECTED`, or `WITHDRAWN`. Resolved and withdrawn disputes are final.
- Every status change is recorded in `dispute_status_history`. Customer responses do not include the internal identifier of the staff member who made the change.
- Amounts are stored in minor units (for example, `129900` is R1,299.00 ZAR). Only the last four digits of a card number are stored.

## Demo data

The `dev` profile seeds two customers. Their passwords are `password`. Do not use the `dev` profile in production, because it creates accounts with known passwords.

| Customer | Email | Cards |
| --- | --- | --- |
| Thandi Mokoena | `thandi@example.com` | 4242, 9001 |
| Ben Adams | `ben@example.com` | 5555 |

## Project layout

```
src/main/kotlin/com/example/dispute/
  api/          REST controllers, DTOs, and the error handler
  config/       Clock bean
  domain/       JPA entities and the dispute status state machine
  repository/   Spring Data repositories
  security/     Customer principal and security configuration
  service/      Dispute and transaction business rules
src/main/resources/db/
  migration/    Schema (V1)
  seed/         Dev-only demo data (V100)
```

## Known limitations

- Staff endpoints are not implemented. The state machine supports staff transitions, but no controller exposes them yet.
- Authentication uses HTTP Basic over plain HTTP in the compose setup. Put the service behind TLS before any real deployment.
