-- Down payment is collected into one bank DFS wallet. The financed amount is not transferred.

ALTER TABLE financing_settings
    ADD COLUMN collection_wallet VARCHAR(20) NULL;

ALTER TABLE financing_customers
    ADD COLUMN dfs_wallet VARCHAR(20) NULL;

ALTER TABLE loan_applications
    ADD COLUMN payment_reference VARCHAR(64) NULL,
    ADD COLUMN payment_from_wallet VARCHAR(20) NULL,
    ADD COLUMN payment_to_wallet VARCHAR(20) NULL,
    ADD COLUMN payment_amount DECIMAL(18,2) NULL,
    ADD COLUMN paid_at TIMESTAMP NULL;
