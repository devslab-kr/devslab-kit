package kr.devslab.kit.admin.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.devslab.kit.tenant.TenantMode;

/**
 * Wire shape for {@code POST /admin/api/v1/tenants}.
 *
 * <p>The admin UI sends just {@code id} + {@code name} when provisioning
 * a tenant, so {@code mode} is optional here. When omitted, the controller
 * uses {@link TenantMode#SINGLE} as the default — the right value for
 * single-application tenant deployments.
 */
public record CreateTenantRequest(
        @NotBlank @Size(max = 64) String id,
        @NotBlank @Size(max = 128) String name,
        TenantMode mode
) {
}
