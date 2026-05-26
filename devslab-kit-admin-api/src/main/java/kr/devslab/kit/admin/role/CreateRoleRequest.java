package kr.devslab.kit.admin.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name
) {
}
