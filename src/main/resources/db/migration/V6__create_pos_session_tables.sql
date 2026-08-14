CREATE TABLE pos_sessions (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    terminal_id UUID NOT NULL REFERENCES terminals(id),
    opened_by_employee_id UUID NOT NULL REFERENCES employees(id),
    closed_by_employee_id UUID REFERENCES employees(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opening_balance_amount NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    closing_expected_amount NUMERIC(19,4),
    closing_counted_amount NUMERIC(19,4),
    closing_variance_amount NUMERIC(19,4),
    opened_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    session_id UUID NOT NULL REFERENCES pos_sessions(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_pos_sessions_terminal_open_unique ON pos_sessions(cafe_id, terminal_id) WHERE status = 'OPEN';
