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
        name = "platform_group",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_group_tenant_code", columnNames = {"tenant_id", "code"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformGroupEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "parent_group_id", columnDefinition = "uuid")
    private UUID parentGroupId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PlatformGroupEntity(UUID id, String tenantId, String code, String name, UUID parentGroupId, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.parentGroupId = parentGroupId;
        this.createdAt = createdAt;
    }
}
