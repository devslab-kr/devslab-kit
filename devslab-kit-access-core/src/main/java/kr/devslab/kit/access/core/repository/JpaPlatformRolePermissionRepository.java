package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformRolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformRolePermissionRepository extends JpaRepository<PlatformRolePermissionEntity, UUID> {

    List<PlatformRolePermissionEntity> findAllByRoleId(UUID roleId);

    Optional<PlatformRolePermissionEntity> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
