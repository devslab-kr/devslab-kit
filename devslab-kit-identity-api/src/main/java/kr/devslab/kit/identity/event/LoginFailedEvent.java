package kr.devslab.kit.identity.event;

import java.time.Instant;
import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.LoginFailureReason;

public record LoginFailedEvent(TenantId tenantId, String loginId, LoginFailureReason reason, Instant occurredAt) {

    public LoginFailedEvent {
        Objects.requireNonNull(tenantId, "LoginFailedEvent tenantId must not be null");
        Objects.requireNonNull(reason, "LoginFailedEvent reason must not be null");
        Objects.requireNonNull(occurredAt, "LoginFailedEvent occurredAt must not be null");
    }
}
