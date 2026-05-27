package kr.devslab.kit.admin.menu;

import jakarta.validation.constraints.Size;

/**
 * Wire shape for {@code PUT /admin/api/v1/menus/{id}}.
 *
 * <p>Every field is optional — only those passed in are applied.
 * Field names match {@link CreateMenuRequest} (and the admin UI).
 */
public record UpdateMenuRequest(
        @Size(max = 255) String label,
        @Size(max = 255) String path,
        Integer displayOrder,
        @Size(max = 128) String requiredPermission,
        @Size(max = 64) String icon
) {
}
