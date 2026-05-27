package kr.devslab.kit.admin.menu;

import java.util.List;
import java.util.UUID;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;

/**
 * Wire shape for menu items returned by {@code /admin/api/v1/menus*}.
 *
 * <p>Two representations live here:
 *
 * <ul>
 *   <li>{@link #flat(PlatformMenuEntity)} — single entity, {@code children == null}.
 *       Used by the flat list endpoint and by {@code GET /menus/{id}}.</li>
 *   <li>{@link #withChildren(PlatformMenuEntity, List)} — entity plus its
 *       already-built child list. Used by {@code GET /menus/tree}.</li>
 * </ul>
 *
 * <p>Field names mirror the admin UI's {@code MenuItem} interface:
 * {@code displayOrder} (not the entity's {@code sortOrder}) and
 * {@code requiredPermission} (not {@code requiredPermissionCode}).
 */
public record MenuResponse(
        UUID id,
        String tenantId,
        String code,
        String label,
        String path,
        UUID parentId,
        int displayOrder,
        String requiredPermission,
        String icon,
        List<MenuResponse> children
) {

    public static MenuResponse flat(PlatformMenuEntity entity) {
        return new MenuResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getPath(),
                entity.getParentId(),
                entity.getSortOrder(),
                entity.getRequiredPermissionCode(),
                entity.getIcon(),
                null
        );
    }

    public static MenuResponse withChildren(PlatformMenuEntity entity, List<MenuResponse> children) {
        return new MenuResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getPath(),
                entity.getParentId(),
                entity.getSortOrder(),
                entity.getRequiredPermissionCode(),
                entity.getIcon(),
                children == null ? List.of() : List.copyOf(children)
        );
    }
}
