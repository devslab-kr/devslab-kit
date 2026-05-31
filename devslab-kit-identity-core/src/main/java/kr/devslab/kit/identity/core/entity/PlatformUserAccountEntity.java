package kr.devslab.kit.identity.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import kr.devslab.kit.identity.UserStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_user_account")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformUserAccountEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "login_id", nullable = false, length = 255)
    private String loginId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "locked", nullable = false)
    private boolean locked;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * When {@code true}, the account holder must rotate their password before
     * using the rest of the system. Set by the bootstrap runner for the first
     * admin (see ADR 0001) and cleared by the self-service change-password flow.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlatformUserAccountEntity(
            UUID id,
            String publicId,
            String tenantId,
            String loginId,
            String email,
            String passwordHash,
            UserStatus status,
            boolean locked,
            String providerType,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.publicId = publicId;
        this.tenantId = tenantId;
        this.loginId = loginId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.locked = locked;
        this.providerType = providerType;
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
