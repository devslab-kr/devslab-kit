package kr.devslab.kit.admin.tenant;

import jakarta.validation.constraints.NotNull;
import kr.devslab.kit.tenant.TenantStatus;

/**
 * Wire shape for {@code PUT /admin/api/v1/tenants/{id}/status}.
 */
public record UpdateTenantStatusRequest(@NotNull TenantStatus status) {
}
