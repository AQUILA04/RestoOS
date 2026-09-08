--liquibase formatted sql

--changeset amelia:010-invitation-rls-public-activate
-- Activation is permitAll (no JWT). Token is an unguessable UUID secret, so
-- SELECT/UPDATE by token must work without app.current_org_id.
-- INSERT/DELETE remain tenant-scoped.

DROP POLICY IF EXISTS tenant_isolation_policy ON invitation_tokens;

CREATE POLICY invitation_tokens_select ON invitation_tokens
    FOR SELECT
    USING (true);

CREATE POLICY invitation_tokens_insert ON invitation_tokens
    FOR INSERT
    WITH CHECK (
        organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
    );

CREATE POLICY invitation_tokens_update ON invitation_tokens
    FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY invitation_tokens_delete ON invitation_tokens
    FOR DELETE
    USING (
        organization_id = NULLIF(current_setting('app.current_org_id', true), '')::uuid
    );

GRANT SELECT, INSERT, UPDATE, DELETE ON invitation_tokens TO restoos_app;
