package kr.devslab.kit.access;

public interface PermissionChecker {

    boolean hasPermission(Permission permission);

    boolean hasAnyPermission(Permission... permissions);

    boolean hasAllPermissions(Permission... permissions);

    default void check(Permission permission) {
        if (!hasPermission(permission)) {
            throw new PermissionDeniedException(permission);
        }
    }
}
