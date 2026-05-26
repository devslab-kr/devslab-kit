package kr.devslab.kit.menu.core.repository;

import java.util.List;
import java.util.UUID;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformMenuRepository extends JpaRepository<PlatformMenuEntity, UUID> {

    List<PlatformMenuEntity> findAllByTenantIdOrderBySortOrderAsc(String tenantId);
}
