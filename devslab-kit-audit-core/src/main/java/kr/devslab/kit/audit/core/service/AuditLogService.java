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
        PlatformAuditLogEntity entity = new PlatformAuditLogEntity(
                UUID.randomUUID(),
                event.action().code(),
                actor == null || actor.userId() == null ? null : actor.userId().value(),
                actor == null || actor.tenantId() == null ? null : actor.tenantId().value(),
                actor == null ? null : actor.displayName(),
                target == null ? null : target.type(),
                target == null ? null : target.id(),
                serializeMetadata(event.metadata()),
                event.occurredAt()
        );
        repository.save(entity);
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
