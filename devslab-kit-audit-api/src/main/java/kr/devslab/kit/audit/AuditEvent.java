package kr.devslab.kit.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Domain event submitted to {@link AuditEventPublisher}.
 *
 * <p>The {@code outcome}, {@code ip}, and {@code userAgent} fields are
 * promoted to first-class as of this revision; older publishers that
 * still construct an event with only {@code action / actor / target /
 * occurredAt / metadata} keep working via the backward-compatible
 * constructor below. {@code AuditLogService} now persists these fields
 * directly into the dedicated columns added by Flyway {@code V9}.
 */
public record AuditEvent(
        AuditAction action,
        AuditActor actor,
        AuditTarget target,
        Instant occurredAt,
        Map<String, Object> metadata,
        AuditOutcome outcome,
        String ip,
        String userAgent
) {

    public AuditEvent {
        Objects.requireNonNull(action, "AuditEvent action must not be null");
        Objects.requireNonNull(occurredAt, "AuditEvent occurredAt must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Backward-compatible constructor for publishers written before
     * outcome / ip / userAgent were first-class fields.
     */
    public AuditEvent(
            AuditAction action,
            AuditActor actor,
            AuditTarget target,
            Instant occurredAt,
            Map<String, Object> metadata
    ) {
        this(action, actor, target, occurredAt, metadata, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AuditAction action;
        private AuditActor actor;
        private AuditTarget target;
        private Instant occurredAt;
        private Map<String, Object> metadata = Map.of();
        private AuditOutcome outcome;
        private String ip;
        private String userAgent;

        public Builder action(AuditAction action) { this.action = action; return this; }
        public Builder actor(AuditActor actor) { this.actor = actor; return this; }
        public Builder target(AuditTarget target) { this.target = target; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public Builder outcome(AuditOutcome outcome) { this.outcome = outcome; return this; }
        public Builder ip(String ip) { this.ip = ip; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }

        public AuditEvent build() {
            return new AuditEvent(action, actor, target, occurredAt, metadata, outcome, ip, userAgent);
        }
    }
}
