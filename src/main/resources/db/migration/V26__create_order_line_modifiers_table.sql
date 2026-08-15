-- V26: Create order line modifiers table for tracking chosen product modifier options on order items
CREATE TABLE order_line_modifiers (
    id UUID PRIMARY KEY,
    order_line_id UUID NOT NULL REFERENCES order_lines(id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups(id),
    modifier_option_id UUID NOT NULL REFERENCES modifier_options(id),
    option_name_snapshot VARCHAR(100) NOT NULL,
    price_delta_snapshot NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_order_line_modifiers_line ON order_line_modifiers(order_line_id);
