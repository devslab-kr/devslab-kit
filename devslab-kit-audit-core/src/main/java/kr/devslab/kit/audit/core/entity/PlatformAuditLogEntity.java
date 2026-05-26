package kr.devslab.kit.audit.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_audit_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformAuditLogEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "action_code", nullable = false, length = 128)
    private String actionCode;

    @Column(name = "actor_user_id", columnDefinition = "uuid")
    private UUID actorUserId;

    @Column(name = "actor_tenant_id", length = 64)
    private String actorTenantId;

    @Column(name = "actor_display_name", length = 255)
    private String actorDisplayName;

    @Column(name = "target_type", length = 128)
    private String targetType;

    @Column(name = "target_id", length = 255)
    private String targetId;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public PlatformAuditLogEntity(
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
        this.id = id;
        this.actionCode = actionCode;
        this.actorUserId = actorUserId;
        this.actorTenantId = actorTenantId;
        this.actorDisplayName = actorDisplayName;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadataJson = metadataJson;
        this.occurredAt = occurredAt;
    }
}
