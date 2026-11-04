-- =====================================================================
-- ORACLE migration: store identification_no without dashes (12 digits)
-- Run as user TESTDB ONLY if CUSTOMER already exists with dashed values.
-- (If you have not created the tables yet, just run the updated
--  02_create_tables.sql instead.)
-- In SQL Developer: F5 (Run Script).
-- =====================================================================

UPDATE CUSTOMER SET identification_no = REPLACE(identification_no, '-', '');

ALTER TABLE CUSTOMER MODIFY (identification_no VARCHAR2(12));

ALTER TABLE CUSTOMER ADD CONSTRAINT CK_CUSTOMER_ident_no
    CHECK (REGEXP_LIKE(identification_no, '^[0-9]{12}$'));

COMMIT;

SELECT customer_id, full_name, identification_no FROM CUSTOMER ORDER BY customer_id;
