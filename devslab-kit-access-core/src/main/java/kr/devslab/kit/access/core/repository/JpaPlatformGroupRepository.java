package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformGroupRepository extends JpaRepository<PlatformGroupEntity, UUID> {

    Optional<PlatformGroupEntity> findByTenantIdAndCode(String tenantId, String code);

    List<PlatformGroupEntity> findAllByTenantId(String tenantId);

    List<PlatformGroupEntity> findAllByParentGroupId(UUID parentGroupId);
}
