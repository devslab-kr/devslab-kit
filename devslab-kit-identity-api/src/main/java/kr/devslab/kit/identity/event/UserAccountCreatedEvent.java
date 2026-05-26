package kr.devslab.kit.identity.event;

import java.time.Instant;
import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record UserAccountCreatedEvent(UserId userId, TenantId tenantId, String loginId, Instant occurredAt) {

    public UserAccountCreatedEvent {
        Objects.requireNonNull(userId, "UserAccountCreatedEvent userId must not be null");
        Objects.requireNonNull(tenantId, "UserAccountCreatedEvent tenantId must not be null");
        Objects.requireNonNull(occurredAt, "UserAccountCreatedEvent occurredAt must not be null");
    }
}
