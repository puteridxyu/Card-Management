-- =====================================================================
-- ORACLE seed data (3 customers)
-- Run as user TESTDB AFTER 02_create_tables.sql. In DBeaver: Alt+X.
-- Fake data only. Safe to re-run (rows are inserted only if missing).
--
--   Aisha  : no documents
--   Budi   : 1 APPROVED document
--   Chandra: 1 REJECTED document + 1 resubmitted PENDING document
--
-- Note: file_path values are NOT backed by real files on disk.
-- Test upload/download through the API (Postman) instead.
-- =====================================================================

-- 1. Customers ----------------------------------------------------------
INSERT INTO CUSTOMER (full_name, identification_no, email)
SELECT v.full_name, v.identification_no, v.email
FROM (
    SELECT 'Aisha Test'   AS full_name, '900101010001' AS identification_no, 'aisha.test@example.com'   AS email FROM dual
    UNION ALL
    SELECT 'Budi Test',    '900202020002', 'budi.test@example.com'    FROM dual
    UNION ALL
    SELECT 'Chandra Test', '900303030003', 'chandra.test@example.com' FROM dual
) v
WHERE NOT EXISTS (
    SELECT 1 FROM CUSTOMER c WHERE c.identification_no = v.identification_no
);

-- 2. Documents ----------------------------------------------------------
INSERT INTO DOCUMENT
    (customer_id, document_type, file_name, stored_file_name, file_path,
     content_type, file_size, verification_status, uploaded_date, updated_date)
SELECT c.customer_id, v.document_type, v.file_name, v.stored_file_name, v.file_path,
       'application/pdf', v.file_size, v.verification_status,
       SYSTIMESTAMP + NUMTODSINTERVAL(v.days_ago, 'DAY'),
       CASE WHEN v.verification_status = 'PENDING'
            THEN SYSTIMESTAMP + NUMTODSINTERVAL(v.days_ago, 'DAY')
            ELSE SYSTIMESTAMP + NUMTODSINTERVAL(v.days_ago + 1, 'DAY') END
FROM (
    SELECT '900202020002' AS identification_no, 'NRIC' AS document_type, 'budi_nric.pdf' AS file_name,
           'seed_budi_nric.pdf' AS stored_file_name, '2026/seed_budi_nric.pdf' AS file_path,
           102400 AS file_size, 'APPROVED' AS verification_status, -10 AS days_ago FROM dual
    UNION ALL
    SELECT '900303030003', 'SALARY_SLIP', 'chandra_salary.pdf',
           'seed_chandra_salary_1.pdf', '2026/seed_chandra_salary_1.pdf', 204800, 'REJECTED', -7 FROM dual
    UNION ALL
    SELECT '900303030003', 'SALARY_SLIP', 'chandra_salary_v2.pdf',
           'seed_chandra_salary_2.pdf', '2026/seed_chandra_salary_2.pdf', 256000, 'PENDING', -2 FROM dual
) v
JOIN CUSTOMER c ON c.identification_no = v.identification_no
WHERE NOT EXISTS (
    SELECT 1 FROM DOCUMENT d WHERE d.stored_file_name = v.stored_file_name
);

-- 3. Document history ---------------------------------------------------
INSERT INTO DOCUMENT_HISTORY
    (document_id, previous_status, new_status, remarks, updated_by, updated_date)
SELECT d.document_id, v.previous_status, v.new_status, v.remarks, v.updated_by,
       SYSTIMESTAMP + NUMTODSINTERVAL(v.days_ago, 'DAY')
FROM (
    SELECT 'seed_budi_nric.pdf' AS stored_file_name, CAST(NULL AS VARCHAR2(20)) AS previous_status,
           'PENDING' AS new_status, CAST(NULL AS VARCHAR2(500)) AS remarks,
           'CUSTOMER' AS updated_by, -10 AS days_ago FROM dual
    UNION ALL
    SELECT 'seed_budi_nric.pdf', 'PENDING', 'APPROVED', 'Document is valid.', 'OFFICER01', -9 FROM dual
    UNION ALL
    SELECT 'seed_chandra_salary_1.pdf', NULL, 'PENDING', NULL, 'CUSTOMER', -7 FROM dual
    UNION ALL
    SELECT 'seed_chandra_salary_1.pdf', 'PENDING', 'REJECTED', 'Uploaded document is unclear.', 'OFFICER01', -6 FROM dual
    UNION ALL
    SELECT 'seed_chandra_salary_2.pdf', NULL, 'PENDING', NULL, 'CUSTOMER', -2 FROM dual
) v
JOIN DOCUMENT d ON d.stored_file_name = v.stored_file_name
WHERE NOT EXISTS (
    SELECT 1 FROM DOCUMENT_HISTORY h
    WHERE h.document_id = d.document_id AND h.new_status = v.new_status
);

COMMIT;

-- 4. Quick check --------------------------------------------------------
SELECT c.full_name, d.document_id, d.document_type, d.verification_status
FROM CUSTOMER c
LEFT JOIN DOCUMENT d ON d.customer_id = c.customer_id
ORDER BY c.customer_id, d.document_id;
