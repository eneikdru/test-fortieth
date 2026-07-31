-- V20260730231111954__add_document_content.sql
-- Add indexed_content column to kb_document_versions table for full-text search indexing

ALTER TABLE kb_document_versions ADD COLUMN indexed_content TEXT;
