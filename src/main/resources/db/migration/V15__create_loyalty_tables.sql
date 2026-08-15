CREATE TABLE customer_loyalty_accounts (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    customer_id UUID NOT NULL,
    current_points INT NOT NULL DEFAULT 0,
    tier_level VARCHAR(30) NOT NULL DEFAULT 'BRONZE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_loyalty_cafe_customer UNIQUE (cafe_id, customer_id)
);

CREATE INDEX idx_loyalty_cafe_customer ON customer_loyalty_accounts(cafe_id, customer_id);

CREATE TABLE loyalty_point_transactions (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    loyalty_account_id UUID NOT NULL REFERENCES customer_loyalty_accounts(id),
    transaction_type VARCHAR(30) NOT NULL,
    points_change INT NOT NULL,
    resulting_points INT NOT NULL,
    reference_order_id UUID,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_loyalty_tx_account ON loyalty_point_transactions(loyalty_account_id, created_at);
