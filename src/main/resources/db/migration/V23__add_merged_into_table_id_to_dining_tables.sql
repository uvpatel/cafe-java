-- V23: Add merged_into_table_id to dining_tables to support dynamic table merging and unmerging
ALTER TABLE dining_tables
ADD COLUMN merged_into_table_id UUID REFERENCES dining_tables(id);

CREATE INDEX idx_dining_tables_merged_into ON dining_tables(merged_into_table_id);
