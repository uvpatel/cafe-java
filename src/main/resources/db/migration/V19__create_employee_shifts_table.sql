CREATE TABLE employee_shifts (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    clock_in_at TIMESTAMP WITH TIME ZONE NOT NULL,
    clock_out_at TIMESTAMP WITH TIME ZONE,
    break_start_at TIMESTAMP WITH TIME ZONE,
    break_end_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_active_employee_shift ON employee_shifts(cafe_id, employee_id) WHERE status IN ('ACTIVE', 'ON_BREAK');
