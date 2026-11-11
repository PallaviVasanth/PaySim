# Architecture and design decisions

```
 Browser (React + TS, Vite build served by nginx)
        |  /api/*  (same origin via nginx or Vite proxy)
        v
 Spring Boot 3 / Java 21
   controller -> service -> repository          (thin controllers, rules in services/domain)
   Spring Security (Basic, stateless, roles)    Springdoc OpenAPI   Actuator
        |  JPA / Hibernate (ddl-auto = validate)
        v
 PostgreSQL 16  <-- schema owned by Flyway (V1__create_payment_schema.sql)
```

## Backend packages

| Package | Responsibility |
|---|---|
| `payment` | `Payment` aggregate, `PaymentStatus` state rules, service, controller, DTOs, simulator, currency policy |
| `audit` | Append-only `payment_audit_events`, recent-activity read model |
| `idempotency` | Key records, request fingerprinting |
| `dashboard` | Aggregate metrics (GROUP BY + LIMIT queries only) |
| `security` | Filter chain, CORS, in-memory users, `/me` |
| `common` | `ApiException`, error envelope, global exception handler |

## State model

```
SUCCEEDED --refund--> REFUNDED
DECLINED  (terminal)
REFUNDED  (terminal)
```
`PaymentStatus.canTransitionTo` is the single source of truth; `Payment.refund()` enforces it. No endpoint accepts a status from the client.

## Idempotency

1. Key format is validated (8-128 chars of `[A-Za-z0-9._:-]`).
2. The request is normalised (trimmed refs, upper-case currency, amount at 2 dp) and hashed with SHA-256.
3. Lookup by key: same hash -> replay stored payment (200, `Idempotent-Replayed: true`); different hash -> 409.
4. Otherwise one DB transaction inserts payment + audit event + idempotency row. `UNIQUE(idempotency_key)` arbitrates races: the loser's transaction rolls back completely (no orphan payment), then it replays the winner's result.

**Limitations:** keys never expire (no TTL/cleanup job); only the payment resource is replayed, not the original HTTP response bytes; key scope is global, not per account (acceptable here because the API is single-tenant).

## Refund concurrency

`SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`) on the payment row serialises concurrent refunds. Exactly one sees `SUCCEEDED`; the rest wait, observe `REFUNDED` and get 409. A `@Version` column is a second guard against lost updates. `ConcurrencyIntegrationTest` exercises both races with 8 parallel requests.

## Money

`BigDecimal` in Java, `NUMERIC(19,2)` in PostgreSQL, validated with `@Digits`/`@DecimalMin`/`@DecimalMax`, plus per-currency precision (JPY: 0 decimals). The UI sends the amount as a JSON number with at most 2 decimals; no floating-point arithmetic is performed anywhere.

## Security posture (portfolio level)

Implemented: server-side validation, role-based authorization (URL rules), BCrypt-hashed in-memory accounts from env vars, stateless Basic auth with JSON 401/403 (no browser popup), CORS allow-list, generic 500s without stack traces, `server.error.*` hardening, DB CHECK/FK/UNIQUE constraints, bounded page size, parameterised queries with LIKE-escaping, no card data fields anywhere, safe fixed-template audit messages, non-root container.

Not implemented / future: real identity provider (OIDC), per-user ownership and multi-tenancy, rate limiting and lockout, HTTPS termination (use a reverse proxy/ingress), secrets manager, audit log immutability at DB level, idempotency-key expiry, metrics/tracing, dependency and container scanning, penetration testing. This project makes **no** PCI-DSS or compliance claims and is not affiliated with PayPal.
