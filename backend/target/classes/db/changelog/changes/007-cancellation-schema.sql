--liquibase formatted sql

--changeset amelia:007-cancellation-schema-columns
ALTER TABLE orders ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN cancelled_by UUID REFERENCES users(id);
ALTER TABLE orders ADD COLUMN cancellation_reason TEXT;
