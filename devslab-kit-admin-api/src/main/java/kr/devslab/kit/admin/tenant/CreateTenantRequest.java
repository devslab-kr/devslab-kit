package kr.devslab.kit.admin.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.devslab.kit.tenant.TenantMode;

public record CreateTenantRequest(
        @NotBlank @Size(max = 64) String id,
        @NotBlank @Size(max = 128) String name,
        @NotNull TenantMode mode
) {
}
