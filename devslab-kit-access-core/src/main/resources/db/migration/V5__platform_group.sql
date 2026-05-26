CREATE TABLE IF NOT EXISTS platform_group (
    id              uuid PRIMARY KEY,
    tenant_id       varchar(64) NOT NULL,
    code            varchar(64) NOT NULL,
    name            varchar(128) NOT NULL,
    parent_group_id uuid REFERENCES platform_group(id) ON DELETE SET NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_group_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_platform_group_parent ON platform_group(parent_group_id);
CREATE INDEX IF NOT EXISTS idx_platform_group_tenant ON platform_group(tenant_id);

CREATE TABLE IF NOT EXISTS platform_user_group (
    id        uuid PRIMARY KEY,
    user_id   uuid NOT NULL,
    group_id  uuid NOT NULL REFERENCES platform_group(id) ON DELETE CASCADE,
    joined_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_user_group_user_group UNIQUE (user_id, group_id)
);
CREATE INDEX IF NOT EXISTS idx_platform_user_group_user ON platform_user_group(user_id);
CREATE INDEX IF NOT EXISTS idx_platform_user_group_group ON platform_user_group(group_id);

CREATE TABLE IF NOT EXISTS platform_group_role (
    id          uuid PRIMARY KEY,
    group_id    uuid NOT NULL REFERENCES platform_group(id) ON DELETE CASCADE,
    role_id     uuid NOT NULL,
    assigned_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_group_role_group_role UNIQUE (group_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_platform_group_role_group ON platform_group_role(group_id);
CREATE INDEX IF NOT EXISTS idx_platform_group_role_role ON platform_group_role(role_id);
