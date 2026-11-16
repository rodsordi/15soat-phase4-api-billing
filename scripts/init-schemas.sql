-- ============================================================================
-- Initialization Script for Unified PostgreSQL Database (garage_db)
-- Creates segregated schemas for all domains in Phase 4 ecosystem
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS work_order;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS keycloak;

GRANT ALL ON SCHEMA work_order TO postgres;
GRANT ALL ON SCHEMA billing TO postgres;
GRANT ALL ON SCHEMA keycloak TO postgres;
