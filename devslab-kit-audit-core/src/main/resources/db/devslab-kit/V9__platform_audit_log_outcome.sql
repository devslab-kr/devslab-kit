-- Add the columns the admin UI's Audit Logs page filters and renders:
--   outcome     -- SUCCESS / FAILURE (nullable; existing rows stay NULL and
--                  the response layer treats them as SUCCESS by default)
--   ip_address  -- network address the actor was on at event time
--   user_agent  -- browser / client identifier
--
-- All three are nullable so existing audit history survives unchanged.

ALTER TABLE platform_audit_log
    ADD COLUMN outcome    VARCHAR(16),
    ADD COLUMN ip_address VARCHAR(64),
    ADD COLUMN user_agent VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_platform_audit_log_outcome ON platform_audit_log(outcome);
