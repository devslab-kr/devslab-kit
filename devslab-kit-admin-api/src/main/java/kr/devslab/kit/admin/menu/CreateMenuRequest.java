package kr.devslab.kit.admin.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Wire shape for {@code POST /admin/api/v1/menus}.
 *
 * <p>Field names mirror what {@code devslab-kit-admin-ui} sends:
 * {@code displayOrder} (not {@code sortOrder}),
 * {@code requiredPermission} (not {@code requiredPermissionCode}),
 * plus the new {@code icon} token (e.g. {@code "pi-users"}). The
 * entity column names underneath are unchanged.
 */
public record CreateMenuRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 255) String path,
        UUID parentId,
        int displayOrder,
        @Size(max = 128) String requiredPermission,
        @Size(max = 64) String icon
) {
}
