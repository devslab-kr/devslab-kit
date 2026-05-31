package kr.devslab.kit.identity.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.UserAccountView;
import kr.devslab.kit.identity.UserStatus;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.event.UserAccountCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class PlatformUserAccountAdminService {

    private final JpaPlatformUserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PlatformUserAccountAdminService(
            JpaPlatformUserAccountRepository repository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public UserAccountView create(TenantId tenantId, String loginId, String email, String rawPassword, String providerType) {
        return create(tenantId, loginId, email, rawPassword, providerType, false);
    }

    @Transactional
    public UserAccountView create(
            TenantId tenantId,
            String loginId,
            String email,
            String rawPassword,
            String providerType,
            boolean mustChangePassword
    ) {
        if (repository.findByTenantIdAndLoginId(tenantId.value(), loginId).isPresent()) {
            throw new IllegalStateException("User already exists: tenant=" + tenantId + " loginId=" + loginId);
        }
        Instant now = Instant.now(clock);
        UUID id = UUID.randomUUID();
        PlatformUserAccountEntity entity = new PlatformUserAccountEntity(
                id,
                "usr_" + id,
                tenantId.value(),
                loginId,
                email,
                rawPassword == null ? null : passwordHasher.hash(rawPassword),
                UserStatus.ACTIVE,
                false,
                providerType == null ? "LOCAL" : providerType,
                now,
                now
        );
        entity.setMustChangePassword(mustChangePassword);
        repository.save(entity);
        eventPublisher.publishEvent(new UserAccountCreatedEvent(UserId.of(id), tenantId, loginId, now));
        return toView(entity);
    }

    @Transactional
    public void lock(UserId id) {
        PlatformUserAccountEntity e = find(id);
        e.setLocked(true);
        e.setUpdatedAt(Instant.now(clock));
    }

    @Transactional
    public void unlock(UserId id) {
        PlatformUserAccountEntity e = find(id);
        e.setLocked(false);
        e.setUpdatedAt(Instant.now(clock));
    }

    @Transactional
    public void setStatus(UserId id, UserStatus status) {
        PlatformUserAccountEntity e = find(id);
        e.setStatus(status);
        e.setUpdatedAt(Instant.now(clock));
    }

    @Transactional
    public void resetPassword(UserId id, String newRawPassword) {
        PlatformUserAccountEntity e = find(id);
        e.setPasswordHash(passwordHasher.hash(newRawPassword));
        e.setUpdatedAt(Instant.now(clock));
    }

    @Transactional
    public void delete(UserId id) {
        repository.deleteById(id.value());
    }

    @Transactional(readOnly = true)
    public List<UserAccountView> listByTenant(TenantId tenantId) {
        return repository.findAll().stream()
                .filter(e -> e.getTenantId().equals(tenantId.value()))
                .map(this::toView)
                .toList();
    }

    private PlatformUserAccountEntity find(UserId id) {
        return repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private UserAccountView toView(PlatformUserAccountEntity e) {
        return new UserAccountView(
                UserId.of(e.getId()),
                PublicId.of(e.getPublicId()),
                TenantId.of(e.getTenantId()),
                e.getLoginId(),
                e.getEmail(),
                e.getStatus(),
                e.isLocked(),
                e.getProviderType()
        );
    }
}
