-- =====================================================================
-- ORACLE: create the TESTDB schema (user)
-- Run ONCE as an admin (SYSTEM / SYS AS SYSDBA), connected to the PDB
-- (e.g. XEPDB1 / ORCLPDB1), NOT the CDB root. In DBeaver: Alt+X.
-- Oracle has no "CREATE DATABASE + USE"; the schema is a user named TESTDB.
-- Change the password to your own. Never commit real passwords.
-- =====================================================================

CREATE USER TESTDB IDENTIFIED BY "TestDb#2026";

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO TESTDB;
GRANT UNLIMITED TABLESPACE TO TESTDB;
