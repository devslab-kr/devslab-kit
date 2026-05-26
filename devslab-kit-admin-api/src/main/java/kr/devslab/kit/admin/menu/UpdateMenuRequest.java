package kr.devslab.kit.admin.menu;

import jakarta.validation.constraints.Size;

public record UpdateMenuRequest(
        @Size(max = 255) String label,
        @Size(max = 255) String path,
        Integer sortOrder,
        @Size(max = 128) String requiredPermissionCode
) {
}
