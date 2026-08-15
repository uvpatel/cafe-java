CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    product_id UUID REFERENCES products(id),
    sku VARCHAR(50) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    unit_of_measure VARCHAR(30) NOT NULL,
    current_stock NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    reorder_threshold NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    cost_per_unit NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_inventory_items_cafe_sku UNIQUE (cafe_id, sku)
);

CREATE INDEX idx_inventory_items_cafe ON inventory_items(cafe_id);

CREATE TABLE inventory_stock_movements (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    movement_type VARCHAR(50) NOT NULL,
    quantity_change NUMERIC(12, 4) NOT NULL,
    resulting_stock NUMERIC(12, 4) NOT NULL,
    reason VARCHAR(255),
    reference_order_id UUID,
    actor_employee_id UUID REFERENCES employees(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_inventory_movements_item ON inventory_stock_movements(inventory_item_id, created_at);
