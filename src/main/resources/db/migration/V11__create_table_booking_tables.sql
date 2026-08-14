CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE table_bookings (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    dining_table_id UUID NOT NULL REFERENCES dining_tables(id),
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    party_size INT NOT NULL DEFAULT 2,
    booking_start TIMESTAMP WITH TIME ZONE NOT NULL,
    booking_end TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_table_bookings_cafe_table ON table_bookings(cafe_id, dining_table_id);
CREATE INDEX idx_table_bookings_start_end ON table_bookings(booking_start, booking_end);

ALTER TABLE table_bookings
ADD CONSTRAINT table_bookings_no_overlap
EXCLUDE USING gist (
    cafe_id WITH =,
    dining_table_id WITH =,
    tstzrange(booking_start, booking_end, '[)') WITH &&
) WHERE (status = 'CONFIRMED');
