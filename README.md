# PaySim — Payment Processing Simulator

A full-stack **simulator** of a payment-processing service: create payments, see them succeed or decline deterministically, refund them, and audit every state change. Built to demonstrate backend engineering fundamentals — transaction-state handling, idempotency, concurrency control, validation, persistence and testing.

> **PaySim is a simulator.** It never moves real money, never accepts card numbers/CVV/bank details, is not affiliated with PayPal or any processor, and makes no PCI-DSS or compliance claims.

## Features

| Area | Implemented |
|---|---|
| Payments | Create with customer ref, order ref, amount, currency, description; UUID transaction id; deterministic success/decline |
| Idempotency | Required `Idempotency-Key`; replay on identical request; 409 on key reuse with different body; race-safe via DB unique constraint |
| History | Search, status filter, sorting, pagination (max 100/page), detail view with audit trail |
| Refunds | **Full refunds only** (partial refunds are *not* supported); state-machine guarded; row-locked against concurrent refunds |
| Dashboard | Totals, per-status counts, status-distribution strip, recent activity — all from DB aggregate queries |
| Audit trail | `PAYMENT_CREATED`, `PAYMENT_REFUNDED` events with previous/new state and a fixed-template safe message |
| Security | Role-based access (OPERATOR / VIEWER), BCrypt, CORS allow-list, safe error envelope, no secrets in code |
| UI | React + TypeScript, **light / dark / system theme switcher**, accessible dialogs (native `<dialog>`), toasts, responsive layout |
| Delivery | Docker Compose, Flyway migrations, OpenAPI/Swagger, GitHub Actions CI |

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Web / Data JPA (Hibernate) / Security / Actuator · Bean Validation · PostgreSQL 16 · Flyway · Springdoc OpenAPI · Maven · JUnit 5 · Mockito · Testcontainers — React 18 · TypeScript · Vite · Lucide icons · plain modern CSS (design tokens) — Docker, Docker Compose, nginx, GitHub Actions.

## Quick start (Docker — recommended)

Requires Docker with Compose v2.

```bash
cp .env.example .env        # then edit the passwords
docker compose up --build
```

- App: http://localhost:3000 (sign in with the operator/viewer accounts from `.env`)
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

First build downloads Maven/npm dependencies and takes a few minutes. Stop with `docker compose down` (add `-v` to wipe the database).

## Local development (without Docker for the apps)

```bash
cp .env.example .env
docker compose up -d db                      # PostgreSQL only

# Backend (Java 21 + Maven 3.9+)
set -a; source .env; set +a
cd backend && mvn spring-boot:run            # http://localhost:8080

# Frontend (Node 20+), in a second terminal
cd frontend && npm install && npm run dev    # http://localhost:5173 (proxies /api to :8080)
```

## Tests and builds

```bash
cd backend  && mvn -B verify                 # unit tests + Testcontainers integration tests (Docker must be running)
cd frontend && npm ci && npm run build       # TypeScript check + production build
```

Backend tests:
- **Unit:** state transitions, simulator rules, currency policy, `Payment` domain behaviour, `PaymentService` (Mockito) incl. idempotency replay/conflict.
- **Integration (real PostgreSQL via Testcontainers):** valid/invalid/negative/malformed amounts, invalid currencies, missing/malformed idempotency keys, replay and key-reuse conflict, declined simulation, refund success / declined / duplicate / unknown / malformed id, role enforcement, 401 without browser popup, error bodies without stack traces, filtering/sorting/pagination/LIKE-escaping, dashboard correctness, and **concurrency** (8 parallel identical creates → 1 payment; 8 parallel refunds → exactly 1 success).

### Verification status of this delivery

| Check | Status |
|---|---|
| Frontend `tsc --noEmit` + `vite build` | **Executed and passed** in the authoring environment |
| Backend `mvn verify` (unit + integration) | **NOT executed** there — Maven Central and Docker were unreachable from the sandbox |
| Backend Java sources | Parsed with `javac`: no syntax errors; unresolved-symbol errors were limited to third-party jars that were unavailable. Not a substitute for a real build |
| `docker compose up` | **Not executed** |

Please run `mvn -B verify` and `docker compose up --build` once locally; if anything fails, the stack trace will point to the exact line to fix.

## Simulator behaviour

| Amount | Result |
|---|---|
| over 10,000.00 | `DECLINED` — `LIMIT_EXCEEDED` |
| ends in `.13` | `DECLINED` — `INSUFFICIENT_FUNDS` |
| ends in `.51` | `DECLINED` — `RISK_CHECK_FAILED` |
| anything else | `SUCCEEDED` |

State model: `SUCCEEDED → REFUNDED`. `DECLINED` and `REFUNDED` are terminal. Clients can never set a status directly.

## Project structure

```
paysim/
├── backend/                     Spring Boot service
│   ├── src/main/java/com/paysim/{payment,audit,idempotency,dashboard,security,common,config}
│   ├── src/main/resources/{application.yml,db/migration/V1__create_payment_schema.sql}
│   ├── src/test/java/com/paysim/{payment,integration}
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                    React + TypeScript (Vite)
│   └── src/{api,components,context,hooks,pages,styles,utils}
├── docs/{API.md,ARCHITECTURE.md}
├── .github/workflows/ci.yml
├── docker-compose.yml  .env.example  Makefile  .gitignore
```

## Documentation

- [API reference with curl examples and error codes](docs/API.md)
- [Architecture, idempotency, concurrency and security decisions](docs/ARCHITECTURE.md)

## Known limitations and remaining production work

- Authentication is HTTP Basic with two env-configured in-memory accounts; the UI keeps credentials in memory only (a page refresh signs you out). A real deployment needs OIDC/OAuth2 and per-user ownership checks — today every authenticated user can see every payment (single-tenant by design).
- No rate limiting, account lockout, HTTPS termination, or secrets manager integration.
- Idempotency keys never expire; replay returns the current payment state, not the original response bytes.
- Only full refunds; no PENDING/PROCESSING states, webhooks, or multi-currency reporting.
- Audit events are append-only by convention (the app never updates them), not enforced by DB permissions.
- No frontend unit/e2e tests (only type-check + build).
- Not security-audited, not PCI-assessed. Do not use with real payment data.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Copy .env.example to .env first` | Run `cp .env.example .env` |
| Backend exits with `Could not resolve placeholder 'PAYSIM_…_PASSWORD'` | Export your `.env` (`set -a; source .env; set +a`) before `mvn spring-boot:run` |
| `Connection refused` to Postgres | `docker compose up -d db`, wait for healthy (`docker compose ps`) |
| Flyway checksum error after editing `V1__…sql` | Never edit applied migrations; add `V2__…` or reset with `docker compose down -v` |
| Integration tests fail to start | Docker must be running for Testcontainers |
| Login shows "Cannot reach the PaySim API" | Backend not up yet, or Vite `VITE_DEV_PROXY_TARGET` wrong |
| Browser CORS error when calling the API directly from another origin | Add that origin to `PAYSIM_CORS_ALLOWED_ORIGINS` |
| Port 5432/8080/3000 in use | Stop the other process or change the host-side port in `docker-compose.yml` |

## Interview summary

*"PaySim is a payment-processing simulator with a Spring Boot / PostgreSQL backend and a React + TypeScript dashboard. The interesting engineering is in correctness under failure: payments are created exactly once per idempotency key, enforced by a database unique constraint inside the same transaction as the insert, so concurrent retries can't double-charge; refunds are serialised with a row lock and guarded by an explicit state machine so two simultaneous refunds can't both succeed. Money is BigDecimal/NUMERIC end to end, the schema is versioned with Flyway, errors use one consistent envelope, and the integration tests run against real PostgreSQL via Testcontainers, including parallel-request tests for both races. I was deliberate about scope: full refunds only, deterministic simulation, and a documented list of what a production system would still need."*

Likely follow-ups: why DB constraint over an in-memory lock (works across instances); pessimistic vs optimistic locking (refunds are low-volume, high-contention-per-row → pessimistic is simpler; `@Version` still guards other writes); what you'd add for production (OIDC, key TTL, outbox/webhooks, rate limiting, observability).
