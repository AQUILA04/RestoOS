--liquibase formatted sql

--changeset amelia:008-app-role-grants failOnError:false splitStatements:false
--comment: Application DB role without BYPASSRLS / superuser. Run as migration owner (postgres).
-- App connects as restoos_app; RLS policies remain enforced. Do NOT grant BYPASSRLS.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'restoos_app') THEN
    CREATE ROLE restoos_app LOGIN PASSWORD 'restoos_app_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO restoos_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO restoos_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO restoos_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO restoos_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO restoos_app;

--changeset amelia:008-order-unique-and-version
ALTER TABLE orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_store_order_number ON orders (store_id, order_number);

--changeset amelia:008-order-counters
CREATE TABLE IF NOT EXISTS order_counters (
    store_id UUID PRIMARY KEY REFERENCES stores(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    last_order_number INT NOT NULL DEFAULT 100,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE order_counters ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_counters FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON order_counters;
CREATE POLICY tenant_isolation_policy ON order_counters FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

--changeset amelia:008-order-items-prepared
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS prepared BOOLEAN NOT NULL DEFAULT FALSE;

--changeset amelia:008-product-modifier-groups
CREATE TABLE IF NOT EXISTS product_modifier_groups (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    modifier_group_id UUID NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, modifier_group_id)
);

CREATE INDEX IF NOT EXISTS idx_pmg_product ON product_modifier_groups(product_id);
CREATE INDEX IF NOT EXISTS idx_pmg_group ON product_modifier_groups(modifier_group_id);

ALTER TABLE product_modifier_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_modifier_groups FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON product_modifier_groups;
CREATE POLICY tenant_isolation_policy ON product_modifier_groups FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

--changeset amelia:008-idempotency-keys
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    store_id UUID,
    endpoint VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_body TEXT,
    status_code INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_tenant_endpoint_key UNIQUE (organization_id, endpoint, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_created ON idempotency_keys(created_at);

ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON idempotency_keys;
CREATE POLICY tenant_isolation_policy ON idempotency_keys FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

--changeset amelia:008-outbox-events
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    store_id UUID,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox_events(published, created_at) WHERE published = FALSE;

ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox_events FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON outbox_events;
CREATE POLICY tenant_isolation_policy ON outbox_events FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

--changeset amelia:008-status-checks
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (
    status IN ('CREATED', 'SENT_TO_KITCHEN', 'PREPARING', 'READY', 'DELIVERED', 'CLOSED', 'CANCELLED')
);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_payment_status;
ALTER TABLE orders ADD CONSTRAINT chk_orders_payment_status CHECK (
    payment_status IN ('UNPAID', 'PAID')
);

-- Soft invariant: CLOSED requires PAID (enforced at app layer + check)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_closed_paid;
ALTER TABLE orders ADD CONSTRAINT chk_orders_closed_paid CHECK (
    NOT (status = 'CLOSED' AND payment_status <> 'PAID')
);

ALTER TABLE restaurant_tables DROP CONSTRAINT IF EXISTS chk_table_status;
ALTER TABLE restaurant_tables ADD CONSTRAINT chk_table_status CHECK (
    status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'OUT_OF_SERVICE')
);

--changeset amelia:008-audit-logs-no-cascade-org
--comment: Prefer RESTRICT over CASCADE for append-only audit trail. App must never DELETE audit_logs.
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_organization_id_fkey;
ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_organization_id_fkey
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT;

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_store_id_fkey;
ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_store_id_fkey
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE SET NULL;
