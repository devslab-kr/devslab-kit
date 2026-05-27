package kr.devslab.kit.menu.core.entity;

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
        name = "platform_menu",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_menu_tenant_code", columnNames = {"tenant_id", "code"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformMenuEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "path", length = 255)
    private String path;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "required_permission_code", length = 128)
    private String requiredPermissionCode;

    @Column(name = "icon", length = 64)
    private String icon;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PlatformMenuEntity(
            UUID id,
            String tenantId,
            String code,
            String label,
            String path,
            UUID parentId,
            int sortOrder,
            String requiredPermissionCode,
            String icon,
            Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.label = label;
        this.path = path;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.requiredPermissionCode = requiredPermissionCode;
        this.icon = icon;
        this.createdAt = createdAt;
    }
}
