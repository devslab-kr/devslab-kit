package kr.devslab.kit.access;

/**
 * One grant path through which a user currently holds a permission.
 *
 * <p>{@code roleCode} is always present (every permission is held via a
 * role). {@code groupCode} is non-null when the role reached the user
 * through a group membership, and null when the role is bound to the
 * user directly.
 *
 * <p>A single {@code permissionCode} can yield multiple grants — the
 * same user may hold the same permission both directly through a role
 * and indirectly via a group. Callers should treat the collection as
 * the full provenance list for that permission.
 */
public record PermissionGrant(
        String permissionCode,
        String roleCode,
        String groupCode
) {

    /**
     * Compact, human-readable rendering used by the admin UI's permission
     * tester: {@code "role:ADMIN"} for direct grants,
     * {@code "group:eng-team>role:ADMIN"} for grants reached through a
     * group.
     */
    public String describe() {
        if (groupCode == null) {
            return "role:" + roleCode;
        }
        return "group:" + groupCode + ">role:" + roleCode;
    }
}
