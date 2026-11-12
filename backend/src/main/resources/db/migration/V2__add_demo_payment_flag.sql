-- Add a flag to distinguish public demo payments from operator payments.
ALTER TABLE payments
ADD COLUMN is_demo BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark only the 35 deterministic fictional demo payments.
UPDATE payments
SET is_demo = TRUE
WHERE id IN (
    SELECT md5('paysim-demo-payment-' || n)::uuid
    FROM generate_series(1, 35) AS n
);