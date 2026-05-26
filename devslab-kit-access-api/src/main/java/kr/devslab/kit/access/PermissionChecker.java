package kr.devslab.kit.access;

import kr.devslab.kit.access.policy.PolicyContext;

public interface PermissionChecker {

    boolean hasPermission(Permission permission);

    boolean hasAnyPermission(Permission... permissions);

    boolean hasAllPermissions(Permission... permissions);

    default boolean isAllowed(Permission permission, String policyName, PolicyContext context) {
        return hasPermission(permission);
    }

    default void check(Permission permission) {
        if (!hasPermission(permission)) {
            throw new PermissionDeniedException(permission);
        }
    }

    default void check(Permission permission, String policyName, PolicyContext context) {
        if (!isAllowed(permission, policyName, context)) {
            throw new PermissionDeniedException(permission);
        }
    }
}
