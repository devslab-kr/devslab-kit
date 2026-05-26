package kr.devslab.kit.access.core.repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlatformPermissionRepository extends JpaRepository<PlatformPermissionEntity, UUID> {

    Optional<PlatformPermissionEntity> findByCode(String code);

    @Query(value = """
            SELECT DISTINCT p.code FROM platform_permission p
            JOIN platform_role_permission rp ON rp.permission_id = p.id
            JOIN platform_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    Set<String> findCodesForUserId(@Param("userId") UUID userId);
}
