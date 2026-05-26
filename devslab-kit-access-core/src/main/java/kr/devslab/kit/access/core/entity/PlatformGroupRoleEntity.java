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
        name = "platform_group_role",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_group_role_group_role", columnNames = {"group_id", "role_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformGroupRoleEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "group_id", columnDefinition = "uuid", nullable = false)
    private UUID groupId;

    @Column(name = "role_id", columnDefinition = "uuid", nullable = false)
    private UUID roleId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    public PlatformGroupRoleEntity(UUID id, UUID groupId, UUID roleId, Instant assignedAt) {
        this.id = id;
        this.groupId = groupId;
        this.roleId = roleId;
        this.assignedAt = assignedAt;
    }
}
