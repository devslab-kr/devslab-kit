package kr.devslab.kit.identity.core.repository;

import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformUserAccountRepository extends JpaRepository<PlatformUserAccountEntity, UUID> {

    Optional<PlatformUserAccountEntity> findByTenantIdAndLoginId(String tenantId, String loginId);

    Optional<PlatformUserAccountEntity> findByPublicId(String publicId);
}
