package kr.devslab.kit.access.core.repository;

import java.util.List;
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

    /**
     * Return every grant path that lands on the given (user, permission code)
     * pair: each row is {permission_code, role_code, group_code (nullable)}.
     *
     * <p>Two unioned branches:
     *
     * <ul>
     *   <li>{@code platform_user_role} — the user holds the role directly;
     *       {@code group_code} is NULL.</li>
     *   <li>{@code platform_user_group} → {@code platform_group_role} — the
     *       user is a member of a group that holds the role; {@code group_code}
     *       carries the group's code.</li>
     * </ul>
     *
     * <p>{@code Object[]} columns: index 0 = permission code, 1 = role code,
     * 2 = group code (null for direct grants). The caller maps to
     * {@code PermissionGrant}.
     */
    @Query(value = """
            SELECT p.code AS permission_code, r.code AS role_code, NULL AS group_code
              FROM platform_permission p
              JOIN platform_role_permission rp ON rp.permission_id = p.id
              JOIN platform_role r              ON r.id = rp.role_id
              JOIN platform_user_role ur        ON ur.role_id = r.id
             WHERE ur.user_id = :userId AND p.code = :permissionCode
            UNION ALL
            SELECT p.code AS permission_code, r.code AS role_code, g.code AS group_code
              FROM platform_permission p
              JOIN platform_role_permission rp ON rp.permission_id = p.id
              JOIN platform_role r              ON r.id = rp.role_id
              JOIN platform_group_role gr       ON gr.role_id = r.id
              JOIN platform_group g             ON g.id = gr.group_id
              JOIN platform_user_group ug       ON ug.group_id = g.id
             WHERE ug.user_id = :userId AND p.code = :permissionCode
            """, nativeQuery = true)
    List<Object[]> findGrantRowsForUserAndPermission(
            @Param("userId") UUID userId,
            @Param("permissionCode") String permissionCode);
}
