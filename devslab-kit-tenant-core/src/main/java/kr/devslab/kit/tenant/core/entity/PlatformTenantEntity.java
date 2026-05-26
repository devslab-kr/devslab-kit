package kr.devslab.kit.tenant.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.devslab.kit.tenant.TenantMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_tenant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private TenantMode mode;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PlatformTenantEntity(String id, String name, TenantMode mode, boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.mode = mode;
        this.active = active;
        this.createdAt = createdAt;
    }
}
