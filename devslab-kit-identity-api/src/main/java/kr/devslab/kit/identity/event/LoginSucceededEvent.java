package kr.devslab.kit.identity.event;

import java.time.Instant;
import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record LoginSucceededEvent(UserId userId, TenantId tenantId, String loginId, Instant occurredAt) {

    public LoginSucceededEvent {
        Objects.requireNonNull(userId, "LoginSucceededEvent userId must not be null");
        Objects.requireNonNull(tenantId, "LoginSucceededEvent tenantId must not be null");
        Objects.requireNonNull(occurredAt, "LoginSucceededEvent occurredAt must not be null");
    }
}
