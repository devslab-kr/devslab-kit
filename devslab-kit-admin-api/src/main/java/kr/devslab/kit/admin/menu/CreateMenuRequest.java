package kr.devslab.kit.admin.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateMenuRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 255) String path,
        UUID parentId,
        int sortOrder,
        @Size(max = 128) String requiredPermissionCode
) {
}
