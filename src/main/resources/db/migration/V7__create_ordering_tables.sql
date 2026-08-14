CREATE TABLE orders (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    pos_session_id UUID NOT NULL REFERENCES pos_sessions(id),
    dining_table_id UUID REFERENCES dining_tables(id),
    order_number VARCHAR(50) NOT NULL,
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE order_lines (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    product_name_snapshot VARCHAR(100) NOT NULL,
    unit_price_snapshot NUMERIC(19,4) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    line_subtotal NUMERIC(19,4) NOT NULL,
    line_total NUMERIC(19,4) NOT NULL,
    is_kitchen_routed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE order_tax_lines (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    tax_name_snapshot VARCHAR(100) NOT NULL,
    rate_percent NUMERIC(7,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL
);

CREATE UNIQUE INDEX idx_orders_table_active_unique ON orders(cafe_id, dining_table_id) WHERE status IN ('DRAFT', 'CONFIRMED', 'SENT_TO_KITCHEN', 'READY');

CREATE UNIQUE INDEX idx_orders_cafe_order_number ON orders(cafe_id, lower(order_number));
