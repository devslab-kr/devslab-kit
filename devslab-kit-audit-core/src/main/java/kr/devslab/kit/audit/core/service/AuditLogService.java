package kr.devslab.kit.audit.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import kr.devslab.kit.audit.AuditActor;
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditOutcome;
import kr.devslab.kit.audit.AuditTarget;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import kr.devslab.kit.core.exception.DevslabKitException;
import org.springframework.transaction.annotation.Transactional;

public class AuditLogService {

    private final JpaPlatformAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(JpaPlatformAuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(AuditEvent event) {
        AuditActor actor = event.actor();
        AuditTarget target = event.target();
        // outcome / ip / userAgent are first-class AuditEvent fields now. We
        // still honour the metadata-map fallback that the field promotion PR
        // (#15) shipped, so legacy publishers built before the SPI extension
        // keep recording their outcome/ip/userAgent into the dedicated columns
        // without a code change.
        var metadata = event.metadata();
        String outcome = event.outcome() != null
                ? event.outcome().name()
                : stringFromMetadata(metadata, "outcome");
        String ipAddress = event.ip() != null
                ? event.ip()
                : stringFromMetadata(metadata, "ip");
        String userAgent = event.userAgent() != null
                ? event.userAgent()
                : stringFromMetadata(metadata, "userAgent");
        PlatformAuditLogEntity entity = new PlatformAuditLogEntity(
                UUID.randomUUID(),
                event.action().code(),
                actor == null || actor.userId() == null ? null : actor.userId().value(),
                actor == null || actor.tenantId() == null ? null : actor.tenantId().value(),
                actor == null ? null : actor.displayName(),
                target == null ? null : target.type(),
                target == null ? null : target.id(),
                serializeMetadata(metadata),
                normalizeOutcome(outcome),
                ipAddress,
                userAgent,
                event.occurredAt()
        );
        repository.save(entity);
    }

    private String stringFromMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Coerce a free-form metadata string into the canonical
     * {@link AuditOutcome} vocabulary the column expects. Unknown values
     * are dropped to NULL so the response layer's "NULL == SUCCESS"
     * default applies, rather than persisting a garbage outcome.
     */
    private String normalizeOutcome(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AuditOutcome.valueOf(raw.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new DevslabKitException("Failed to serialize audit metadata", e);
        }
    }
}
