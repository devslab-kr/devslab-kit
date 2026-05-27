package kr.devslab.kit.audit.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import kr.devslab.kit.audit.AuditActor;
import kr.devslab.kit.audit.AuditEvent;
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
        // outcome / ip / userAgent are honoured if the publisher stuffs them
        // into the AuditEvent metadata map under those keys; otherwise the row
        // stays NULL and the response layer treats it as a SUCCESS row. The
        // AuditEvent record itself doesn't carry these yet — adding them as
        // first-class fields lives in a follow-up PR.
        var metadata = event.metadata();
        String outcome = stringFromMetadata(metadata, "outcome");
        String ipAddress = stringFromMetadata(metadata, "ip");
        String userAgent = stringFromMetadata(metadata, "userAgent");
        PlatformAuditLogEntity entity = new PlatformAuditLogEntity(
                UUID.randomUUID(),
                event.action().code(),
                actor == null || actor.userId() == null ? null : actor.userId().value(),
                actor == null || actor.tenantId() == null ? null : actor.tenantId().value(),
                actor == null ? null : actor.displayName(),
                target == null ? null : target.type(),
                target == null ? null : target.id(),
                serializeMetadata(metadata),
                outcome,
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
