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
        name = "platform_role_permission",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_role_permission_role_perm", columnNames = {"role_id", "permission_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformRolePermissionEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "role_id", columnDefinition = "uuid", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", columnDefinition = "uuid", nullable = false)
    private UUID permissionId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    public PlatformRolePermissionEntity(UUID id, UUID roleId, UUID permissionId, Instant grantedAt) {
        this.id = id;
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.grantedAt = grantedAt;
    }
}
