package kr.devslab.kit.admin.audit;

import java.time.Instant;
import java.util.UUID;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;

public record AuditLogResponse(
        UUID id,
        String actionCode,
        UUID actorUserId,
        String actorTenantId,
        String actorDisplayName,
        String targetType,
        String targetId,
        String metadataJson,
        Instant occurredAt
) {

    public static AuditLogResponse from(PlatformAuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getActionCode(),
                entity.getActorUserId(),
                entity.getActorTenantId(),
                entity.getActorDisplayName(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getMetadataJson(),
                entity.getOccurredAt()
        );
    }
}
