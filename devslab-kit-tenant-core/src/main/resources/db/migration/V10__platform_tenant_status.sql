-- Replace the boolean `active` flag with a richer TenantStatus enum
-- (ACTIVE / SUSPENDED / ARCHIVED). Existing rows are back-filled in place:
--   active = true  -> ACTIVE
--   active = false -> SUSPENDED  (treat as "paused", not as "soft-deleted")
--
-- Done in three steps so the rewrite stays online-safe:
--   1. add nullable status column,
--   2. back-fill from active,
--   3. swap constraints + indexes and drop active.

ALTER TABLE platform_tenant
    ADD COLUMN status VARCHAR(16);

UPDATE platform_tenant
   SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'SUSPENDED' END
 WHERE status IS NULL;

ALTER TABLE platform_tenant
    ALTER COLUMN status SET NOT NULL;

DROP INDEX IF EXISTS idx_platform_tenant_active;
CREATE INDEX IF NOT EXISTS idx_platform_tenant_status ON platform_tenant(status);

ALTER TABLE platform_tenant
    DROP COLUMN active;
