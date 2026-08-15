ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX uk_payments_cafe_idempotency ON payments(cafe_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
