package kr.devslab.kit.admin.menu;

import java.util.UUID;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;

public record MenuResponse(
        UUID id,
        String tenantId,
        String code,
        String label,
        String path,
        UUID parentId,
        int sortOrder,
        String requiredPermissionCode
) {

    public static MenuResponse from(PlatformMenuEntity entity) {
        return new MenuResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getPath(),
                entity.getParentId(),
                entity.getSortOrder(),
                entity.getRequiredPermissionCode()
        );
    }
}
