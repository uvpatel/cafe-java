CREATE TABLE export_jobs (
    id UUID PRIMARY KEY,
    cafe_id UUID NOT NULL REFERENCES cafes(id),
    export_type VARCHAR(30) NOT NULL,
    format VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_path VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_export_jobs_cafe_status ON export_jobs(cafe_id, status);
