package kr.devslab.kit.access.core.service;

import java.util.Set;
import kr.devslab.kit.access.Permission;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.identity.CurrentUserProvider;
import org.springframework.transaction.annotation.Transactional;

public class DefaultPermissionChecker implements PermissionChecker {

    private final CurrentUserProvider currentUserProvider;
    private final JpaPlatformPermissionRepository permissionRepository;

    public DefaultPermissionChecker(
            CurrentUserProvider currentUserProvider,
            JpaPlatformPermissionRepository permissionRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(Permission permission) {
        return currentPermissionCodes().contains(permission.code());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(Permission... permissions) {
        Set<String> codes = currentPermissionCodes();
        for (Permission permission : permissions) {
            if (codes.contains(permission.code())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAllPermissions(Permission... permissions) {
        Set<String> codes = currentPermissionCodes();
        for (Permission permission : permissions) {
            if (!codes.contains(permission.code())) {
                return false;
            }
        }
        return true;
    }

    private Set<String> currentPermissionCodes() {
        return currentUserProvider.current()
                .map(user -> permissionRepository.findCodesForUserId(user.id().value()))
                .orElse(Set.of());
    }
}
