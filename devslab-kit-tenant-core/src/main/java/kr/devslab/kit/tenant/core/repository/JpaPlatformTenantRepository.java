package kr.devslab.kit.tenant.core.repository;

import kr.devslab.kit.tenant.core.entity.PlatformTenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformTenantRepository extends JpaRepository<PlatformTenantEntity, String> {
}
