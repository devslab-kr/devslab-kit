package kr.devslab.kit.menu.core.service;

import java.util.List;
import java.util.Objects;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.menu.MenuItem;
import kr.devslab.kit.menu.MenuTree;

public class PermissionBasedMenuFilter {

    private final PermissionChecker permissionChecker;

    public PermissionBasedMenuFilter(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    public MenuTree filter(MenuTree tree) {
        List<MenuItem> visibleRoots = tree.roots().stream()
                .map(this::filterItem)
                .filter(Objects::nonNull)
                .toList();
        return new MenuTree(visibleRoots);
    }

    private MenuItem filterItem(MenuItem item) {
        if (item.requiredPermission().isPresent()
                && !permissionChecker.hasPermission(item.requiredPermission().get())) {
            return null;
        }
        List<MenuItem> visibleChildren = item.children().stream()
                .map(this::filterItem)
                .filter(Objects::nonNull)
                .toList();
        return new MenuItem(
                item.id(),
                item.code(),
                item.label(),
                item.path(),
                item.requiredPermission(),
                visibleChildren
        );
    }
}
