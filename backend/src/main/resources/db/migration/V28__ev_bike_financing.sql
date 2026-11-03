-- EV bike / consumer financing. Separate from payment maker-checker (approval_requests).

CREATE TABLE financing_settings (
    id                                  BIGINT       NOT NULL PRIMARY KEY,
    kibor_percent                       DECIMAL(8,4) NOT NULL,
    kibor_floor_percent                 DECIMAL(8,4) NOT NULL,
    suggested_bank_commission_percent   DECIMAL(8,4) NOT NULL,
    bank_commission_max_percent         DECIMAL(8,4) NOT NULL,
    down_payment_min_percent            DECIMAL(8,4) NOT NULL,
    down_payment_max_percent            DECIMAL(8,4) NOT NULL,
    down_payment_options                VARCHAR(200) NOT NULL,
    tenure_months                       VARCHAR(100) NOT NULL,
    dbr_threshold_percent               DECIMAL(8,4) NOT NULL,
    card_limit_basis                    VARCHAR(32)  NOT NULL,
    card_limit_factor_percent           DECIMAL(8,4) NOT NULL,
    late_payment_percent                DECIMAL(8,4) NOT NULL,
    quotation_max_age_days              INT          NOT NULL,
    formula_code                        VARCHAR(64)  NULL,
    formula_note                        VARCHAR(500) NULL,
    product_enabled                     BOOLEAN      NOT NULL,
    updated_at                          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO financing_settings (
    id, kibor_percent, kibor_floor_percent, suggested_bank_commission_percent,
    bank_commission_max_percent, down_payment_min_percent, down_payment_max_percent,
    down_payment_options, tenure_months, dbr_threshold_percent, card_limit_basis,
    card_limit_factor_percent, late_payment_percent, quotation_max_age_days,
    formula_code, formula_note, product_enabled
) VALUES (
    1, 12.0000, 12.0000, 3.0000,
    5.0000, 30.0000, 50.0000,
    '30,40,50', '12,24,36', 40.0000, 'UNSET',
    5.0000, 2.0000, 7,
    'REDUCING_BALANCE_EMI',
    'Provisional reducing-balance EMI until JS Bank supplies the financing formula (BRD open item 1). Customer quotes use KIBOR only. Bank commission is stored separately and applied by financing operations.',
    TRUE
);

CREATE TABLE product_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_category_code (code)
);

INSERT INTO product_categories (code, name, active, sort_order)
VALUES ('MOTORCYCLE', 'Motorcycles / EV bikes', TRUE, 1);

CREATE TABLE financing_products (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_party_id BIGINT        NOT NULL,
    category_id       BIGINT        NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    description       VARCHAR(2000) NULL,
    specifications    VARCHAR(4000) NULL,
    price             DECIMAL(18,2) NOT NULL,
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fp_party FOREIGN KEY (merchant_party_id) REFERENCES parties(id),
    CONSTRAINT fk_fp_category FOREIGN KEY (category_id) REFERENCES product_categories(id)
);

CREATE INDEX idx_fp_merchant ON financing_products (merchant_party_id, active);
CREATE INDEX idx_fp_category ON financing_products (category_id, active);

CREATE TABLE financing_product_images (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT       NOT NULL,
    stored_path  VARCHAR(500) NOT NULL,
    content_type VARCHAR(120) NULL,
    original_name VARCHAR(255) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fpi_product FOREIGN KEY (product_id) REFERENCES financing_products(id)
);

CREATE INDEX idx_fpi_product ON financing_product_images (product_id);

CREATE TABLE dealer_authorizations (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    party_id    BIGINT      NOT NULL,
    category_id BIGINT      NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_da_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_da_category FOREIGN KEY (category_id) REFERENCES product_categories(id),
    UNIQUE KEY uk_dealer_category (party_id, category_id)
);

CREATE TABLE financing_whitelist (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cnic        VARCHAR(13)  NOT NULL,
    mobile      VARCHAR(20)  NOT NULL,
    full_name   VARCHAR(200) NULL,
    segment_code VARCHAR(60) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fin_whitelist_cnic (cnic)
);

CREATE INDEX idx_fin_whitelist_mobile ON financing_whitelist (mobile);

CREATE TABLE financing_customers (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cnic             VARCHAR(13)  NOT NULL,
    mobile           VARCHAR(20)  NOT NULL,
    full_name        VARCHAR(200) NOT NULL,
    session_token    VARCHAR(64)  NULL,
    session_expires_at TIMESTAMP  NULL,
    otp_verified_at  TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fin_customer_cnic (cnic),
    UNIQUE KEY uk_fin_customer_mobile (mobile)
);

CREATE TABLE loan_document_rules (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_code   VARCHAR(64)  NOT NULL,
    document_label  VARCHAR(200) NOT NULL,
    applicability   VARCHAR(40)  NOT NULL,
    mandatory       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_loan_doc_code (document_code)
);

INSERT INTO loan_document_rules (document_code, document_label, applicability, mandatory, sort_order) VALUES
('EV_QUOTATION', 'EV bike quotation (not older than the configured age)', 'ALL', TRUE, 10),
('CF1_DECLARATION', 'CF-1 declaration', 'ALL', TRUE, 20),
('BORROWER_FACT_SHEET', 'Borrower basic fact sheet', 'ALL', TRUE, 30),
('KFS', 'Key fact statement', 'ALL', TRUE, 40),
('KFS_SIGNED', 'Digitally signed key fact statement (attachment 2B2)', 'ALL', TRUE, 50),
('PERSONAL_GUARANTEE', 'Personal guarantee', 'ALL', TRUE, 60),
('HYPOTHECATION', 'Hypothecation agreement', 'ALL', TRUE, 70),
('SALARY_SLIPS', 'Last six months salary slips', 'SALARY', TRUE, 80),
('EMPLOYMENT_CONFIRMATION', 'Employment confirmation', 'SALARY', TRUE, 90),
('EMPLOYER_RECOMMENDATION', 'Employer recommendation letter', 'CORPORATE_REFERRED', TRUE, 100),
('EMPLOYER_UNDERTAKING', 'Employer undertaking for salary deduction and remittance', 'CORPORATE_REFERRED', TRUE, 110),
('CORPORATE_GUARANTEE_OR_TRIPARTITE', 'Corporate guarantee or tripartite agreement', 'CORPORATE_REFERRED', TRUE, 120),
('PDC', 'Post-dated cheques', 'CORPORATE_REFERRED', TRUE, 130),
('PDC_SELF', 'Post-dated cheques', 'SELF_EMPLOYED', TRUE, 140),
('GUARANTOR_CHEQUE', 'Guarantor post-dated cheque (principal plus markup)', 'SELF_EMPLOYED', TRUE, 150);

CREATE TABLE loan_applications (
    id                              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    public_id                       VARCHAR(36)   NOT NULL,
    reference_no                    VARCHAR(32)   NOT NULL,
    customer_id                     BIGINT        NOT NULL,
    merchant_party_id               BIGINT        NOT NULL,
    product_id                      BIGINT        NOT NULL,
    category_id                     BIGINT        NOT NULL,
    employment_type                 VARCHAR(40)   NOT NULL,
    monthly_income                  DECIMAL(18,2) NOT NULL,
    existing_installments           DECIMAL(18,2) NOT NULL,
    card_limit_amount               DECIMAL(18,2) NULL,
    product_title                   VARCHAR(200)  NOT NULL,
    product_price                   DECIMAL(18,2) NOT NULL,
    requested_down_payment_percent  DECIMAL(8,4)  NOT NULL,
    confirmed_down_payment_percent  DECIMAL(8,4)  NULL,
    tenure_months                   INT           NOT NULL,
    quotation_date                  DATE          NOT NULL,
    indicative_kibor_percent        DECIMAL(8,4)  NULL,
    indicative_monthly_installment  DECIMAL(18,2) NULL,
    indicative_financed_amount      DECIMAL(18,2) NULL,
    bank_commission_percent         DECIMAL(8,4)  NULL,
    approved_annual_rate            DECIMAL(8,4)  NULL,
    approved_monthly_installment    DECIMAL(18,2) NULL,
    approved_down_payment_amount    DECIMAL(18,2) NULL,
    approved_financed_amount        DECIMAL(18,2) NULL,
    approved_total_payable          DECIMAL(18,2) NULL,
    commission_set_by_account_id    BIGINT        NULL,
    commission_set_at               TIMESTAMP     NULL,
    forwarded_by_account_id         BIGINT        NULL,
    forwarded_at                    TIMESTAMP     NULL,
    ops_remarks                     VARCHAR(1000) NULL,
    checker_remarks                 VARCHAR(1000) NULL,
    status                          VARCHAR(40)   NOT NULL,
    payment_status                  VARCHAR(32)   NOT NULL,
    biometric_ref                   VARCHAR(80)   NULL,
    biometric_source                VARCHAR(40)   NULL,
    biometric_at                    TIMESTAMP     NULL,
    ecib_status                     VARCHAR(40)   NOT NULL,
    risk_rating_status              VARCHAR(40)   NOT NULL,
    dbr_percent                     DECIMAL(8,4)  NULL,
    dbr_passed                      BOOLEAN       NULL,
    dbr_note                        VARCHAR(500)  NULL,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_la_customer FOREIGN KEY (customer_id) REFERENCES financing_customers(id),
    CONSTRAINT fk_la_merchant FOREIGN KEY (merchant_party_id) REFERENCES parties(id),
    CONSTRAINT fk_la_product FOREIGN KEY (product_id) REFERENCES financing_products(id),
    CONSTRAINT fk_la_category FOREIGN KEY (category_id) REFERENCES product_categories(id),
    UNIQUE KEY uk_la_public (public_id),
    UNIQUE KEY uk_la_ref (reference_no)
);

CREATE INDEX idx_la_status ON loan_applications (status, created_at);
CREATE INDEX idx_la_customer ON loan_applications (customer_id);
CREATE INDEX idx_la_merchant ON loan_applications (merchant_party_id);

CREATE TABLE loan_events (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id    BIGINT        NOT NULL,
    event_type        VARCHAR(40)   NOT NULL,
    actor_account_id  BIGINT        NULL,
    actor_label       VARCHAR(200)  NOT NULL,
    remark            VARCHAR(1000) NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_le_app FOREIGN KEY (application_id) REFERENCES loan_applications(id)
);

CREATE INDEX idx_le_app ON loan_events (application_id, created_at);

CREATE TABLE loan_documents (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT       NOT NULL,
    document_code   VARCHAR(64)  NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    stored_path     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(120) NULL,
    uploaded_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ld_app FOREIGN KEY (application_id) REFERENCES loan_applications(id)
);

CREATE INDEX idx_ld_app ON loan_documents (application_id, document_code);

CREATE TABLE financing_notifications (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT        NOT NULL,
    application_id  BIGINT        NOT NULL,
    status_code     VARCHAR(40)   NOT NULL,
    title           VARCHAR(200)  NOT NULL,
    body            VARCHAR(1000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         TIMESTAMP     NULL,
    CONSTRAINT fk_fn_customer FOREIGN KEY (customer_id) REFERENCES financing_customers(id),
    CONSTRAINT fk_fn_app FOREIGN KEY (application_id) REFERENCES loan_applications(id)
);

CREATE INDEX idx_fn_customer ON financing_notifications (customer_id, created_at);
