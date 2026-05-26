package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformRoleRepository extends JpaRepository<PlatformRoleEntity, UUID> {

    Optional<PlatformRoleEntity> findByTenantIdAndCode(String tenantId, String code);

    List<PlatformRoleEntity> findAllByTenantId(String tenantId);
}
