-- Create index for querying models with data_present = 1 for a specific tenant
CREATE INDEX IF NOT EXISTS idx_tenant_data_present ON model (tenant_id, data_present);

-- Add comment explaining the purpose
COMMENT ON INDEX idx_tenant_data_present IS 'Index to optimize queries that filter models by tenant_id and data_present';

-- Analyze the table to update statistics
ANALYZE model; 