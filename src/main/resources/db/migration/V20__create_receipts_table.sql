CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    receipt_number VARCHAR(50) NOT NULL,
    subtotal_amount NUMERIC(19,4) NOT NULL,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    service_charge_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    tax_breakdown_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_receipts_cafe_number ON receipts(cafe_id, lower(receipt_number));
CREATE UNIQUE INDEX idx_receipts_order_id ON receipts(order_id);
