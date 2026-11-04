-- =====================================================================
-- ORACLE - OPTIONAL: pagination test data (25 extra documents for Budi)
-- Run as user TESTDB AFTER 03_seed_data.sql. In DBeaver: Alt+X.
-- Statuses cycle PENDING / APPROVED / REJECTED.
-- With the 3 seeded documents there will be 28 in total (3 pages of 10).
-- Safe to re-run. Cleanup statements are at the bottom.
-- =====================================================================

-- 1. Documents ----------------------------------------------------------
INSERT INTO DOCUMENT
    (customer_id, document_type, file_name, stored_file_name, file_path,
     content_type, file_size, verification_status, uploaded_date, updated_date)
SELECT c.customer_id,
       CASE MOD(n.n, 3) WHEN 0 THEN 'NRIC' WHEN 1 THEN 'SALARY_SLIP' ELSE 'BANK_STATEMENT' END,
       'page_test_' || LPAD(n.n, 3, '0') || '.pdf',
       'seed_page_' || LPAD(n.n, 3, '0') || '.pdf',
       '2026/seed_page_' || LPAD(n.n, 3, '0') || '.pdf',
       'application/pdf',
       50000 + n.n * 1000,
       CASE MOD(n.n, 3) WHEN 0 THEN 'PENDING' WHEN 1 THEN 'APPROVED' ELSE 'REJECTED' END,
       SYSTIMESTAMP - NUMTODSINTERVAL(n.n, 'MINUTE'),
       CASE MOD(n.n, 3) WHEN 0 THEN SYSTIMESTAMP - NUMTODSINTERVAL(n.n, 'MINUTE')
                        ELSE SYSTIMESTAMP - NUMTODSINTERVAL(n.n - 1, 'MINUTE') END
FROM (SELECT LEVEL AS n FROM dual CONNECT BY LEVEL <= 25) n
CROSS JOIN (SELECT customer_id FROM CUSTOMER WHERE identification_no = '900202020002') c
WHERE NOT EXISTS (
    SELECT 1 FROM DOCUMENT d
    WHERE d.stored_file_name = 'seed_page_' || LPAD(n.n, 3, '0') || '.pdf'
);

-- 2. History: first entry (NULL -> PENDING) for every page-test document -
INSERT INTO DOCUMENT_HISTORY (document_id, previous_status, new_status, remarks, updated_by, updated_date)
SELECT d.document_id, NULL, 'PENDING', NULL, 'CUSTOMER', d.uploaded_date
FROM DOCUMENT d
WHERE d.stored_file_name LIKE 'seed\_page\_%' ESCAPE '\'
  AND NOT EXISTS (
      SELECT 1 FROM DOCUMENT_HISTORY h
      WHERE h.document_id = d.document_id AND h.new_status = 'PENDING'
  );

-- 3. History: review entry for APPROVED / REJECTED documents ------------
INSERT INTO DOCUMENT_HISTORY (document_id, previous_status, new_status, remarks, updated_by, updated_date)
SELECT d.document_id, 'PENDING', d.verification_status,
       CASE d.verification_status WHEN 'APPROVED' THEN 'Document is valid.' ELSE 'Uploaded document is unclear.' END,
       'OFFICER01', d.updated_date
FROM DOCUMENT d
WHERE d.stored_file_name LIKE 'seed\_page\_%' ESCAPE '\'
  AND d.verification_status IN ('APPROVED', 'REJECTED')
  AND NOT EXISTS (
      SELECT 1 FROM DOCUMENT_HISTORY h
      WHERE h.document_id = d.document_id AND h.new_status = d.verification_status
  );

COMMIT;

-- 4. Quick check --------------------------------------------------------
SELECT verification_status, COUNT(*) AS total FROM DOCUMENT GROUP BY verification_status;

-- =====================================================================
-- CLEANUP (uncomment and run to remove the pagination test data)
-- =====================================================================
-- DELETE FROM DOCUMENT_HISTORY WHERE document_id IN (SELECT document_id FROM DOCUMENT WHERE stored_file_name LIKE 'seed\_page\_%' ESCAPE '\');
-- DELETE FROM DOCUMENT WHERE stored_file_name LIKE 'seed\_page\_%' ESCAPE '\';
-- COMMIT;
