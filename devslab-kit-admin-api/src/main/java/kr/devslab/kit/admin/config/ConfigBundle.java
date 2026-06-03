package kr.devslab.kit.admin.config;

import java.util.List;

/**
 * Portable, environment-independent snapshot of the kit's platform config — keyed
 * entirely by natural codes, never DB UUIDs — so it can be exported from one
 * environment and imported into another (or committed to git and applied on deploy).
 * See ADR 0003.
 *
 * <p><strong>Definitional</strong> config (permissions, roles + their permission codes,
 * menus) is always present. <strong>Users</strong> are operational data and are only
 * included when explicitly requested (export {@code includeUsers=true}); even then no
 * secret is ever carried — only login id, email, status and assigned role codes. History
 * (audit logs) is never included; ABAC policies are code, not data.
 */
public record ConfigBundle(
        int version,
        String tenantId,
        List<PermissionDef> permissions,
        List<RoleDef> roles,
        List<MenuDef> menus,
        List<UserDef> users
) {

    /** Bump when the bundle shape changes incompatibly. */
    public static final int CURRENT_VERSION = 1;

    /** Null-normalises every list so a partially-populated bundle (e.g. hand-pasted JSON) is safe. */
    public ConfigBundle {
        permissions = permissions == null ? List.of() : permissions;
        roles = roles == null ? List.of() : roles;
        menus = menus == null ? List.of() : menus;
        users = users == null ? List.of() : users;
    }

    /** Definitional-only bundle (no users) — the common case. */
    public ConfigBundle(
            int version,
            String tenantId,
            List<PermissionDef> permissions,
            List<RoleDef> roles,
            List<MenuDef> menus
    ) {
        this(version, tenantId, permissions, roles, menus, List.of());
    }

    public record PermissionDef(String code, String description) {
    }

    /** A role and the permission <em>codes</em> it grants (resolved cross-environment). */
    public record RoleDef(String code, String name, List<String> permissionCodes) {
        public RoleDef {
            permissionCodes = permissionCodes == null ? List.of() : permissionCodes;
        }
    }

    /** A menu item; {@code parentCode} / {@code requiredPermissionCode} reference by code. */
    public record MenuDef(
            String code,
            String parentCode,
            String label,
            String path,
            String icon,
            String requiredPermissionCode,
            int displayOrder
    ) {
    }

    /**
     * A user account — natural key is {@code loginId} within the tenant. Carries no
     * password (transported users are created with no usable password and must have one
     * set by an admin) and no environment-specific public id. {@code roleCodes} are the
     * roles to assign by code.
     */
    public record UserDef(String loginId, String email, String status, List<String> roleCodes) {
        public UserDef {
            roleCodes = roleCodes == null ? List.of() : roleCodes;
        }
    }
}
