CREATE TABLE IF NOT EXISTS platform_role (
    id          uuid PRIMARY KEY,
    tenant_id   varchar(64) NOT NULL,
    code        varchar(64) NOT NULL,
    name        varchar(128) NOT NULL,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_role_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS platform_permission (
    id          uuid PRIMARY KEY,
    code        varchar(128) NOT NULL UNIQUE,
    description varchar(512),
    created_at  timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform_user_role (
    id          uuid PRIMARY KEY,
    user_id     uuid NOT NULL,
    role_id     uuid NOT NULL,
    tenant_id   varchar(64) NOT NULL,
    assigned_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_user_role_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_platform_user_role_user ON platform_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_platform_user_role_role ON platform_user_role(role_id);

CREATE TABLE IF NOT EXISTS platform_role_permission (
    id            uuid PRIMARY KEY,
    role_id       uuid NOT NULL,
    permission_id uuid NOT NULL,
    granted_at    timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_role_permission_role_perm UNIQUE (role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_platform_role_permission_role ON platform_role_permission(role_id);
