CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    code VARCHAR(50) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(19,4) NOT NULL,
    min_order_amount NUMERIC(19,4) DEFAULT 0.0000,
    max_redemptions INT,
    times_redeemed INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE coupon_redemptions (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    coupon_id UUID NOT NULL REFERENCES coupons(id),
    order_id UUID NOT NULL REFERENCES orders(id),
    customer_id UUID,
    redeemed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_coupons_cafe_code ON coupons(cafe_id, lower(code));

CREATE UNIQUE INDEX idx_coupon_redemptions_order ON coupon_redemptions(order_id);
