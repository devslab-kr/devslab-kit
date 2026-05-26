package kr.devslab.kit.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(
        AuditAction action,
        AuditActor actor,
        AuditTarget target,
        Instant occurredAt,
        Map<String, Object> metadata
) {

    public AuditEvent {
        Objects.requireNonNull(action, "AuditEvent action must not be null");
        Objects.requireNonNull(occurredAt, "AuditEvent occurredAt must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
