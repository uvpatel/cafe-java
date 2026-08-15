CREATE TABLE order_split_payments (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    split_index INT NOT NULL,
    payment_method_id UUID NOT NULL REFERENCES payment_methods(id),
    amount NUMERIC(12, 4) NOT NULL,
    tip_amount NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    payer_name VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_order_split_index UNIQUE (order_id, split_index)
);

CREATE INDEX idx_order_split_order ON order_split_payments(order_id);
