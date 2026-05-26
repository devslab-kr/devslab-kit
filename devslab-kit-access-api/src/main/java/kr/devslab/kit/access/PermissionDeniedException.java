package kr.devslab.kit.access;

import kr.devslab.kit.core.exception.DevslabKitException;

public class PermissionDeniedException extends DevslabKitException {

    private final Permission permission;

    public PermissionDeniedException(Permission permission) {
        super("Permission denied: " + permission.code());
        this.permission = permission;
    }

    public Permission permission() {
        return permission;
    }
}
