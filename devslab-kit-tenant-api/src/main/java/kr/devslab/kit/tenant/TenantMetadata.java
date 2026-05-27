package kr.devslab.kit.tenant;

import java.time.Instant;
import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;

public record TenantMetadata(
        TenantId id,
        String name,
        TenantMode mode,
        TenantStatus status,
        Instant createdAt
) {

    public TenantMetadata {
        Objects.requireNonNull(id, "TenantMetadata id must not be null");
        Objects.requireNonNull(mode, "TenantMetadata mode must not be null");
        Objects.requireNonNull(status, "TenantMetadata status must not be null");
        Objects.requireNonNull(createdAt, "TenantMetadata createdAt must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("TenantMetadata name must not be null or blank");
        }
    }
}
