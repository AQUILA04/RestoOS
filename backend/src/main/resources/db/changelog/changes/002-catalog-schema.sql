--liquibase formatted sql

--changeset amelia:002-catalog-schema-tables
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_86 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    min_selection INT NOT NULL DEFAULT 0,
    max_selection INT NOT NULL DEFAULT 1,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE modifier_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset amelia:002-catalog-schema-rls
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON categories FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON products FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

ALTER TABLE modifier_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE modifier_groups FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON modifier_groups FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

ALTER TABLE modifier_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE modifier_options FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON modifier_options FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);
