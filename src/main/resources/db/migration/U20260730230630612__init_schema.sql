-- U20260730230630612__init_schema.sql
-- Manual disaster recovery procedure / undo for the initial schema.
-- Note: As Flyway Community Edition does not support automatic undo, this script is provided
-- for manual execution by system administrators in disaster recovery scenarios.

-- DROP ALL TABLES IN REVERSE ORDER OF CREATION / DEPENDENCIES
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS document_versions;
DROP TABLE IF EXISTS document_metadata;
DROP TABLE IF EXISTS document_tags;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS users;
