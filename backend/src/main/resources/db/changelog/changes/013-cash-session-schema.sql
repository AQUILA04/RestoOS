--liquibase formatted sql

--changeset amelia:013-cash-sessions
CREATE TABLE cash_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    opened_by_user_id UUID NOT NULL REFERENCES users(id),
    closed_by_user_id UUID REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    opening_float DECIMAL(10,2),
    closing_notes TEXT,
    report_snapshot TEXT
);

CREATE UNIQUE INDEX uq_cash_session_open_per_cashier
    ON cash_sessions (store_id, opened_by_user_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_cash_sessions_store_opened_at
    ON cash_sessions (store_id, opened_at DESC);

--changeset amelia:013-cash-sessions-rls
ALTER TABLE cash_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON cash_sessions FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);

--changeset amelia:013-payments-cash-session-id
ALTER TABLE payments ADD COLUMN IF NOT EXISTS cash_session_id UUID REFERENCES cash_sessions(id);
CREATE INDEX IF NOT EXISTS idx_payments_cash_session_id ON payments (cash_session_id);
