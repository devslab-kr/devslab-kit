package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformUserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformUserRoleRepository extends JpaRepository<PlatformUserRoleEntity, UUID> {

    List<PlatformUserRoleEntity> findAllByUserId(UUID userId);

    Optional<PlatformUserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
