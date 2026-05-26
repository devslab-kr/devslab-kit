package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformUserGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformUserGroupRepository extends JpaRepository<PlatformUserGroupEntity, UUID> {

    List<PlatformUserGroupEntity> findAllByUserId(UUID userId);

    List<PlatformUserGroupEntity> findAllByGroupId(UUID groupId);

    Optional<PlatformUserGroupEntity> findByUserIdAndGroupId(UUID userId, UUID groupId);

    void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}
