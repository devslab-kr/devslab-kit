package kr.devslab.kit.menu.core.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.devslab.kit.access.Permission;
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.menu.MenuItem;
import kr.devslab.kit.menu.MenuTree;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;

public class MenuTreeBuilder {

    public MenuTree build(List<PlatformMenuEntity> entities) {
        Map<UUID, List<PlatformMenuEntity>> byParent = entities.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(PlatformMenuEntity::getParentId));

        List<MenuItem> roots = entities.stream()
                .filter(e -> e.getParentId() == null)
                .sorted(Comparator.comparingInt(PlatformMenuEntity::getSortOrder))
                .map(entity -> toItem(entity, byParent))
                .toList();

        return new MenuTree(roots);
    }

    private MenuItem toItem(PlatformMenuEntity entity, Map<UUID, List<PlatformMenuEntity>> byParent) {
        List<MenuItem> children = byParent.getOrDefault(entity.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(PlatformMenuEntity::getSortOrder))
                .map(child -> toItem(child, byParent))
                .toList();

        Optional<Permission> requiredPermission = Optional.ofNullable(entity.getRequiredPermissionCode())
                .map(Permission::of);

        return new MenuItem(
                MenuId.of(entity.getId()),
                entity.getCode(),
                entity.getLabel(),
                entity.getPath(),
                entity.getIcon(),
                requiredPermission,
                children
        );
    }
}
