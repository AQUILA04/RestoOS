--liquibase formatted sql

--changeset amelia:012-org-logo
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS logo_url VARCHAR(1000);

--changeset amelia:012-product-avg-prep
ALTER TABLE products ADD COLUMN IF NOT EXISTS avg_prep_minutes INT NOT NULL DEFAULT 15;
