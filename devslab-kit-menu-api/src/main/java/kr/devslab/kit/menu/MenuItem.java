package kr.devslab.kit.menu;

import java.util.List;
import java.util.Optional;
import kr.devslab.kit.access.Permission;
import kr.devslab.kit.core.id.MenuId;

public record MenuItem(
        MenuId id,
        String code,
        String label,
        String path,
        Optional<Permission> requiredPermission,
        List<MenuItem> children
) {

    public MenuItem {
        requiredPermission = requiredPermission == null ? Optional.empty() : requiredPermission;
        children = children == null ? List.of() : List.copyOf(children);
    }
}
