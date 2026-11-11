# PaySim API reference

Interactive docs: `http://localhost:8080/swagger-ui.html` (or `:3000/swagger-ui.html` via the nginx proxy).
OpenAPI JSON: `/v3/api-docs`. Disable both with `PAYSIM_SWAGGER_ENABLED=false`.

**Auth:** HTTP Basic. `OPERATOR` can read, create and refund; `VIEWER` is read-only. `/actuator/health` is public.

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/v1/payments` | OPERATOR | Create a payment (requires `Idempotency-Key`) |
| GET | `/api/v1/payments` | any | Search, filter, sort, paginate |
| GET | `/api/v1/payments/{id}` | any | Payment + audit trail |
| POST | `/api/v1/payments/{id}/refund` | OPERATOR | Full refund |
| GET | `/api/v1/dashboard/summary` | any | Aggregated metrics + recent activity |
| GET | `/api/v1/me` | any | Current user and roles |
| GET | `/actuator/health` | public | Health (includes DB connectivity) |

## Examples

```bash
AUTH="operator:change-me-operator-password"   # whatever you set in .env

# Create (201). Re-running with the same key and body returns 200 + Idempotent-Replayed: true
curl -i -u "$AUTH" -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c2d7e-9b0a-4a55-8d3e-1f2a3b4c5d6e" \
  -d '{"customerReference":"cust_1042","orderReference":"ORD-2026-0001","amount":49.90,"currency":"USD","description":"Annual plan"}'

# Same key, different body -> 409 IDEMPOTENCY_KEY_REUSED
# No header            -> 400 MISSING_IDEMPOTENCY_KEY

# Declined by the simulator (amount ends in .13)
curl -u "$AUTH" -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" -H "Idempotency-Key: demo-declined-0001" \
  -d '{"customerReference":"cust_1","orderReference":"ORD-2","amount":10.13,"currency":"EUR"}'

# List: filters + sort + paging (size is capped at 100)
curl -u "$AUTH" "http://localhost:8080/api/v1/payments?status=SUCCEEDED&q=ord&sort=amount&direction=desc&page=0&size=20"

# Refund
curl -u "$AUTH" -X POST http://localhost:8080/api/v1/payments/<id>/refund
```

## Simulator rules (deterministic)

| Condition | Outcome |
|---|---|
| amount > 10,000.00 | `DECLINED` / `LIMIT_EXCEEDED` |
| amount ends in `.13` | `DECLINED` / `INSUFFICIENT_FUNDS` |
| amount ends in `.51` | `DECLINED` / `RISK_CHECK_FAILED` |
| otherwise | `SUCCEEDED` |

Supported currencies: USD, EUR, GBP, INR, JPY, CAD, AUD (JPY allows no decimals). Amount: 0.01 to 1,000,000.00.

## Error format

```json
{
  "timestamp": "2026-10-13T09:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "One or more fields are invalid.",
  "path": "/api/v1/payments",
  "fieldErrors": [{ "field": "amount", "message": "Amount must be at least 0.01." }]
}
```

| HTTP | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `MISSING_IDEMPOTENCY_KEY`, `INVALID_IDEMPOTENCY_KEY`, `INVALID_PARAMETER`, `INVALID_SORT` | Bad input |
| 401 | `UNAUTHENTICATED` | Missing/invalid credentials |
| 403 | `FORBIDDEN` | Viewer attempting a write |
| 404 | `PAYMENT_NOT_FOUND`, `NOT_FOUND` | Unknown resource |
| 409 | `IDEMPOTENCY_KEY_REUSED`, `INVALID_STATE_TRANSITION`, `CONCURRENT_MODIFICATION` | Conflicts |
| 422 | `UNSUPPORTED_CURRENCY`, `INVALID_AMOUNT_PRECISION` | Valid JSON, business rule violated |
| 500 | `INTERNAL_ERROR` | Unexpected; details only in server logs |
