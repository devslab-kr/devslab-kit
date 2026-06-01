CREATE TABLE IF NOT EXISTS platform_audit_log (
    id                 uuid PRIMARY KEY,
    action_code        varchar(128) NOT NULL,
    actor_user_id      uuid,
    actor_tenant_id    varchar(64),
    actor_display_name varchar(255),
    target_type        varchar(128),
    target_id          varchar(255),
    metadata_json      text,
    occurred_at        timestamp with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_platform_audit_log_tenant_time ON platform_audit_log(actor_tenant_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_platform_audit_log_actor ON platform_audit_log(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_platform_audit_log_action ON platform_audit_log(action_code);
