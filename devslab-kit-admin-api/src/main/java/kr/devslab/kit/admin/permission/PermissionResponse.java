package kr.devslab.kit.admin.permission;

import java.time.Instant;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformPermissionEntity;

public record PermissionResponse(UUID id, String code, String description, Instant createdAt) {

    public static PermissionResponse from(PlatformPermissionEntity entity) {
        return new PermissionResponse(entity.getId(), entity.getCode(), entity.getDescription(), entity.getCreatedAt());
    }
}
