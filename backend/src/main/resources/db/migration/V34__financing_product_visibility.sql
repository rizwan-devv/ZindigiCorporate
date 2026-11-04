-- Back office chooses which merchant products customers can see.
-- Rows already on sale stay visible. New products start hidden.

ALTER TABLE financing_products
    ADD COLUMN shown_to_customers BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE financing_products SET shown_to_customers = TRUE;
