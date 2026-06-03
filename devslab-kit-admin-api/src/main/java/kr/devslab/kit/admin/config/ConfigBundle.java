package kr.devslab.kit.admin.config;

import java.util.List;

/**
 * Portable, environment-independent snapshot of the kit's <em>definitional</em>
 * platform config — keyed entirely by natural codes, never DB UUIDs — so it can be
 * exported from one environment and imported into another (or committed to git and
 * applied on deploy). See ADR 0003.
 *
 * <p>Scope: permissions, roles (+ their permission codes), and menus. Operational data
 * (users, assignments) and history (audit logs) are deliberately excluded; ABAC policies
 * are code, not data.
 */
public record ConfigBundle(
        int version,
        String tenantId,
        List<PermissionDef> permissions,
        List<RoleDef> roles,
        List<MenuDef> menus
) {

    /** Bump when the bundle shape changes incompatibly. */
    public static final int CURRENT_VERSION = 1;

    public record PermissionDef(String code, String description) {
    }

    /** A role and the permission <em>codes</em> it grants (resolved cross-environment). */
    public record RoleDef(String code, String name, List<String> permissionCodes) {
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
}
