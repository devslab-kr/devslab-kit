package kr.devslab.kit.access.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "platform_user_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_user_role_user_role", columnNames = {"user_id", "role_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformUserRoleEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "role_id", columnDefinition = "uuid", nullable = false)
    private UUID roleId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    public PlatformUserRoleEntity(UUID id, UUID userId, UUID roleId, String tenantId, Instant assignedAt) {
        this.id = id;
        this.userId = userId;
        this.roleId = roleId;
        this.tenantId = tenantId;
        this.assignedAt = assignedAt;
    }
}
