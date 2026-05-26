package kr.devslab.kit.admin.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
        @NotBlank @Size(max = 128) String code,
        @Size(max = 512) String description
) {
}
