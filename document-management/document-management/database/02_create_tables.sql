-- =====================================================================
-- ORACLE: tables, constraints and indexes
-- Run as user TESTDB (connect to the PDB with TESTDB / your password).
-- In DBeaver: Alt+X (Execute SQL Script).
-- Run ONCE. To start over, drop in this order:
--   DROP TABLE DOCUMENT_HISTORY; DROP TABLE DOCUMENT; DROP TABLE CUSTOMER;
-- Needs Oracle 12c or later (identity columns).
-- VARCHAR2(n CHAR) is used so Unicode names work (AL32UTF8) and plain
-- Java String fields validate cleanly with Hibernate.
-- =====================================================================

-- 1. CUSTOMER -----------------------------------------------------------
CREATE TABLE CUSTOMER (
    customer_id       NUMBER(10)          GENERATED ALWAYS AS IDENTITY,
    full_name         VARCHAR2(100 CHAR)  NOT NULL,
    identification_no VARCHAR2(12)        NOT NULL,
    email             VARCHAR2(100)       NOT NULL,
    created_date      TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_CUSTOMER PRIMARY KEY (customer_id),
    CONSTRAINT UQ_CUSTOMER_ident_no UNIQUE (identification_no),
    CONSTRAINT CK_CUSTOMER_ident_no CHECK (REGEXP_LIKE(identification_no, '^[0-9]{12}$')),
    CONSTRAINT UQ_CUSTOMER_email UNIQUE (email)
);

-- 2. DOCUMENT -----------------------------------------------------------
CREATE TABLE DOCUMENT (
    document_id         NUMBER(10)          GENERATED ALWAYS AS IDENTITY,
    customer_id         NUMBER(10)          NOT NULL,
    document_type       VARCHAR2(30)        NOT NULL,
    file_name           VARCHAR2(255 CHAR)  NOT NULL,
    stored_file_name    VARCHAR2(100)       NOT NULL,
    file_path           VARCHAR2(500)       NOT NULL,
    content_type        VARCHAR2(100)       NOT NULL,
    file_size           NUMBER(19)          NOT NULL,
    verification_status VARCHAR2(20)        DEFAULT 'PENDING' NOT NULL,
    uploaded_date       TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    updated_date        TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_DOCUMENT PRIMARY KEY (document_id),
    CONSTRAINT UQ_DOCUMENT_stored_name UNIQUE (stored_file_name),
    CONSTRAINT FK_DOCUMENT_CUSTOMER FOREIGN KEY (customer_id) REFERENCES CUSTOMER (customer_id),
    CONSTRAINT CK_DOCUMENT_status CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT CK_DOCUMENT_file_size CHECK (file_size > 0)
);

-- 3. DOCUMENT_HISTORY ---------------------------------------------------
CREATE TABLE DOCUMENT_HISTORY (
    history_id      NUMBER(10)          GENERATED ALWAYS AS IDENTITY,
    document_id     NUMBER(10)          NOT NULL,
    previous_status VARCHAR2(20),
    new_status      VARCHAR2(20)        NOT NULL,
    remarks         VARCHAR2(500 CHAR),
    updated_by      VARCHAR2(50)        NOT NULL,
    updated_date    TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_DOCUMENT_HISTORY PRIMARY KEY (history_id),
    CONSTRAINT FK_HISTORY_DOCUMENT FOREIGN KEY (document_id) REFERENCES DOCUMENT (document_id),
    CONSTRAINT CK_HISTORY_prev_status CHECK (previous_status IS NULL OR previous_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT CK_HISTORY_new_status CHECK (new_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- 4. Indexes ------------------------------------------------------------
CREATE INDEX IX_DOCUMENT_customer_id ON DOCUMENT (customer_id);
CREATE INDEX IX_DOCUMENT_status_uploaded ON DOCUMENT (verification_status, uploaded_date);
CREATE INDEX IX_HISTORY_document_updated ON DOCUMENT_HISTORY (document_id, updated_date);

-- 5. Quick check --------------------------------------------------------
SELECT table_name FROM user_tables ORDER BY table_name;
