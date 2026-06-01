CREATE TABLE IF NOT EXISTS platform_tenant (
    id         varchar(64) PRIMARY KEY,
    name       varchar(128) NOT NULL,
    mode       varchar(16)  NOT NULL,
    active     boolean      NOT NULL DEFAULT true,
    created_at timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_platform_tenant_active ON platform_tenant(active);
