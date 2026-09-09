--liquibase formatted sql

--changeset amelia:009-invitation-tokens
CREATE TABLE IF NOT EXISTS invitation_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE invitation_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitation_tokens FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation_policy ON invitation_tokens;
CREATE POLICY tenant_isolation_policy ON invitation_tokens FOR ALL USING (
    organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
);
