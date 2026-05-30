package kr.devslab.kit.admin.tenant;

import java.time.Instant;
import kr.devslab.kit.tenant.TenantMetadata;
import kr.devslab.kit.tenant.TenantMode;
import kr.devslab.kit.tenant.TenantStatus;

/**
 * Wire shape for {@code /admin/api/v1/tenants} responses.
 *
 * <p>Unwraps {@link TenantMetadata}'s {@code TenantId} into a plain
 * {@code String} so the admin UI's {@code Tenant} interface
 * ({@code id: string}) talks to the same field without a wrapper
 * object — the other tenanted resources (users, roles, groups, menus)
 * still expose {@code id: {value}} because those identifiers are
 * UUIDs and the wrapping is what carries the type, but tenants use
 * the plain slug everywhere so the unwrap is unambiguous.
 */
public record TenantResponse(
        String id,
        String name,
        TenantMode mode,
        TenantStatus status,
        Instant createdAt
) {

    public static TenantResponse from(TenantMetadata metadata) {
        return new TenantResponse(
                metadata.id().value(),
                metadata.name(),
                metadata.mode(),
                metadata.status(),
                metadata.createdAt()
        );
    }
}
