--liquibase formatted sql

--changeset amelia:011-orders-created-by
ALTER TABLE orders ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);

--changeset amelia:011-org-mobile-money-label
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS mobile_money_label VARCHAR(100);

--changeset amelia:011-payments-amount-tendered
ALTER TABLE payments ADD COLUMN IF NOT EXISTS amount_tendered DECIMAL(10,2);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS change_amount DECIMAL(10,2);
