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
            WHERE rp.role_id IN (
                SELECT ur.role_id FROM platform_user_role ur WHERE ur.user_id = :userId
                UNION
                SELECT gr.role_id FROM platform_group_role gr
                JOIN platform_user_group ug ON ug.group_id = gr.group_id
                WHERE ug.user_id = :userId
            )
            """, nativeQuery = true)
    Set<String> findCodesForUserId(@Param("userId") UUID userId);
}
