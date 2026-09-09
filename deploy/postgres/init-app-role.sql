-- Runs as restoos_admin on first container init.
-- Application role intentionally has NO SUPERUSER / BYPASSRLS.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'restoos_app') THEN
    CREATE ROLE restoos_app LOGIN PASSWORD 'restoos_app_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  END IF;
END
$$;

DO $$
BEGIN
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO restoos_app', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO restoos_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO restoos_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO restoos_app;
