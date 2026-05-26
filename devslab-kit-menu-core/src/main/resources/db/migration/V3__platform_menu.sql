CREATE TABLE IF NOT EXISTS platform_menu (
    id                       uuid PRIMARY KEY,
    tenant_id                varchar(64) NOT NULL,
    code                     varchar(64) NOT NULL,
    label                    varchar(255) NOT NULL,
    path                     varchar(255),
    parent_id                uuid REFERENCES platform_menu(id) ON DELETE CASCADE,
    sort_order               int NOT NULL DEFAULT 0,
    required_permission_code varchar(128),
    created_at               timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_menu_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX IF NOT EXISTS idx_platform_menu_parent ON platform_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_platform_menu_tenant ON platform_menu(tenant_id);
