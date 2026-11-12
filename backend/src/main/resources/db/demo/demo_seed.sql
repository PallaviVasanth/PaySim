
-- ============================================================
-- PaySim - Fictional Demo Payment Data
-- 20 SUCCEEDED | 10 DECLINED | 5 REFUNDED
-- 35 Payments | 40 Audit Events
--
-- Safe to execute repeatedly.
-- Does not delete or modify existing payment records.
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 1. Insert 35 demo payments
-- ------------------------------------------------------------

WITH demo_rows AS (
    SELECT
        n,
        md5('paysim-demo-payment-' || n)::uuid AS id,

        'DEMO-CUST-' || lpad(n::text, 3, '0')
            AS customer_reference,

        'DEMO-ORDER-' || lpad(n::text, 3, '0')
            AS order_reference,

        CASE
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 0
                THEN 12000.00
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 1
                THEN 125.13
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 2
                THEN 225.51
            ELSE 500.00 + (n * 37.25)
        END::numeric(19,2) AS amount,

        CASE
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 0
                THEN 'LIMIT_EXCEEDED'
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 1
                THEN 'INSUFFICIENT_FUNDS'
            WHEN n BETWEEN 21 AND 30 AND n % 3 = 2
                THEN 'RISK_CHECK_FAILED'
            ELSE NULL
        END AS decline_reason,

        CASE
            WHEN n BETWEEN 21 AND 30 THEN 'DECLINED'
            WHEN n BETWEEN 31 AND 35 THEN 'REFUNDED'
            ELSE 'SUCCEEDED'
        END AS status,

        now() - ((36 - n) * interval '2 days') AS created_at

    FROM generate_series(1, 35) AS n
)

INSERT INTO payments (
    id,
    customer_reference,
    order_reference,
    amount,
    currency,
    description,
    status,
    decline_reason,
    created_at,
    updated_at,
    refunded_at,
    is_demo
)
SELECT
    id,
    customer_reference,
    order_reference,
    amount,
    'INR',
    'PaySim demo transaction #' || n,
    status,
    decline_reason,
    created_at,

    CASE
        WHEN status = 'REFUNDED'
            THEN created_at + interval '1 hour'
        ELSE NULL
    END,
    TRUE

FROM demo_rows
ON CONFLICT (id) DO NOTHING;
-- Mark the exact deterministic demo records as public demo data.
UPDATE payments
SET is_demo = TRUE
WHERE id IN (
    SELECT md5('paysim-demo-payment-' || n)::uuid
    FROM generate_series(1, 35) AS n
);


-- ------------------------------------------------------------
-- 2. Insert payment creation audit events
-- ------------------------------------------------------------

INSERT INTO payment_audit_events (
    payment_id,
    event_type,
    previous_status,
    new_status,
    message,
    created_at
)
SELECT
    p.id,
    'PAYMENT_CREATED',
    NULL,
    p.status,
    'Demo payment created',
    p.created_at

FROM payments p

WHERE p.customer_reference LIKE 'DEMO-CUST-%'
  AND p.order_reference LIKE 'DEMO-ORDER-%'

  AND NOT EXISTS (
      SELECT 1
      FROM payment_audit_events a
      WHERE a.payment_id = p.id
        AND a.event_type = 'PAYMENT_CREATED'
  );


-- ------------------------------------------------------------
-- 3. Insert refund audit events
-- ------------------------------------------------------------

INSERT INTO payment_audit_events (
    payment_id,
    event_type,
    previous_status,
    new_status,
    message,
    created_at
)
SELECT
    p.id,
    'PAYMENT_REFUNDED',
    'SUCCEEDED',
    'REFUNDED',
    'Demo payment refunded',
    p.refunded_at

FROM payments p

WHERE p.customer_reference LIKE 'DEMO-CUST-%'
  AND p.order_reference LIKE 'DEMO-ORDER-%'
  AND p.status = 'REFUNDED'

  AND NOT EXISTS (
      SELECT 1
      FROM payment_audit_events a
      WHERE a.payment_id = p.id
        AND a.event_type = 'PAYMENT_REFUNDED'
  );


COMMIT;

-- ============================================================
-- End of PaySim demo seed
-- ============================================================
