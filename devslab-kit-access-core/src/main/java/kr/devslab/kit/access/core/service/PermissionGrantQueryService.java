package kr.devslab.kit.access.core.service;

import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.PermissionGrant;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns the grant path(s) by which a user currently holds a given
 * permission — used by the admin UI's diagnostics page to explain
 * <em>why</em> a permission check resolved the way it did.
 *
 * <p>Lives next to {@link DefaultPermissionChecker} but is separate
 * so the {@code PermissionChecker} contract stays thin (the checker
 * answers yes/no on the hot path; this service is for human-facing
 * explanations and isn't on the hot path).
 */
public class PermissionGrantQueryService {

    private final JpaPlatformPermissionRepository permissionRepository;

    public PermissionGrantQueryService(JpaPlatformPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissionGrant> findGrantsFor(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            return List.of();
        }
        return permissionRepository.findGrantRowsForUserAndPermission(userId, permissionCode).stream()
                .map(row -> new PermissionGrant(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2]))
                .toList();
    }
}
