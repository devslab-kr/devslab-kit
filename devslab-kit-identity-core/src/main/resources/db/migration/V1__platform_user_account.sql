CREATE TABLE IF NOT EXISTS platform_user_account (
    id              uuid PRIMARY KEY,
    public_id       varchar(64) NOT NULL UNIQUE,
    tenant_id       varchar(64) NOT NULL,
    login_id        varchar(255) NOT NULL,
    email           varchar(255),
    password_hash   varchar(255),
    status          varchar(32) NOT NULL,
    locked          boolean NOT NULL DEFAULT false,
    provider_type   varchar(32) NOT NULL DEFAULT 'LOCAL',
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    updated_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_user_account_tenant_login UNIQUE (tenant_id, login_id)
);

CREATE INDEX IF NOT EXISTS idx_platform_user_account_email ON platform_user_account(email);
