package kr.devslab.kit.admin.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;

/**
 * Wire shape for {@code GET /admin/api/v1/audit-logs} entries.
 *
 * <p>Field names mirror the admin UI's {@code AuditLog} interface:
 * the entity's {@code actionCode} / {@code actorUserId} / {@code actorTenantId}
 * / {@code actorDisplayName} / {@code metadataJson} columns map to wire fields
 * {@code action} / {@code actorId} / {@code tenantId} / {@code actorLogin} /
 * {@code payload}. Outcome defaults to {@code SUCCESS} on rows written before
 * the column existed (NULL outcome).
 */
public record AuditLogResponse(
        UUID id,
        String tenantId,
        UUID actorId,
        String actorLogin,
        String action,
        String targetType,
        String targetId,
        String outcome,
        String ip,
        String userAgent,
        Map<String, Object> payload,
        Instant occurredAt
) {

    public static AuditLogResponse from(PlatformAuditLogEntity entity, ObjectMapper objectMapper) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorTenantId(),
                entity.getActorUserId(),
                entity.getActorDisplayName(),
                entity.getActionCode(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getOutcome() == null ? "SUCCESS" : entity.getOutcome(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                parsePayload(entity.getMetadataJson(), objectMapper),
                entity.getOccurredAt()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parsePayload(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            // Corrupt or non-JSON payload — return the raw text under a single
            // key so the UI can still surface it for inspection rather than
            // failing the whole row.
            return Map.of("raw", json);
        }
    }
}
