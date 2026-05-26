package kr.devslab.kit.identity.core.service;

import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.UserAccountView;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import org.springframework.transaction.annotation.Transactional;

public class PlatformUserAccountService {

    private final JpaPlatformUserAccountRepository repository;

    public PlatformUserAccountService(JpaPlatformUserAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UserAccountView> findById(UUID id) {
        return repository.findById(id).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccountView> findByPublicId(String publicId) {
        return repository.findByPublicId(publicId).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Optional<UserAccountView> findByTenantAndLoginId(String tenantId, String loginId) {
        return repository.findByTenantIdAndLoginId(tenantId, loginId).map(this::toView);
    }

    private UserAccountView toView(PlatformUserAccountEntity entity) {
        return new UserAccountView(
                UserId.of(entity.getId()),
                PublicId.of(entity.getPublicId()),
                TenantId.of(entity.getTenantId()),
                entity.getLoginId(),
                entity.getEmail(),
                entity.getStatus(),
                entity.isLocked(),
                entity.getProviderType()
        );
    }
}
