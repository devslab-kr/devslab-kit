package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformGroupRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformGroupRoleRepository extends JpaRepository<PlatformGroupRoleEntity, UUID> {

    List<PlatformGroupRoleEntity> findAllByGroupId(UUID groupId);

    Optional<PlatformGroupRoleEntity> findByGroupIdAndRoleId(UUID groupId, UUID roleId);

    void deleteByGroupIdAndRoleId(UUID groupId, UUID roleId);
}
