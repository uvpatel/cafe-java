-- V25: Create indexes on product_modifier_group_links for bi-directional lookup optimization
CREATE INDEX IF NOT EXISTS idx_product_modifier_links_product ON product_modifier_group_links(product_id);
CREATE INDEX IF NOT EXISTS idx_product_modifier_links_group ON product_modifier_group_links(group_id);
