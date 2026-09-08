--liquibase formatted sql

--changeset amelia:003-store-catalog-schema-tables
CREATE TABLE store_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    override_price DECIMAL(10,2),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_store_products UNIQUE (store_id, product_id)
);

--changeset amelia:003-store-catalog-schema-rls
ALTER TABLE store_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE store_products FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON store_products FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);
