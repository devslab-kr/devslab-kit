package kr.devslab.kit.access.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlatformRoleRepository extends JpaRepository<PlatformRoleEntity, UUID> {

    Optional<PlatformRoleEntity> findByTenantIdAndCode(String tenantId, String code);

    List<PlatformRoleEntity> findAllByTenantId(String tenantId);

    /**
     * Every role code currently bound to the user — both directly via
     * {@code platform_user_role} and indirectly via group memberships
     * ({@code platform_user_group} → {@code platform_group_role}).
     *
     * <p>Used at login to populate {@code CurrentUser.roles} so the JWT
     * payload and the login response carry the user's effective role set.
     */
    @Query(value = """
            SELECT DISTINCT r.code FROM platform_role r
            WHERE r.id IN (
                SELECT ur.role_id FROM platform_user_role ur WHERE ur.user_id = :userId
                UNION
                SELECT gr.role_id FROM platform_group_role gr
                JOIN platform_user_group ug ON ug.group_id = gr.group_id
                WHERE ug.user_id = :userId
            )
            """, nativeQuery = true)
    Set<String> findRoleCodesForUserId(@Param("userId") UUID userId);
}
