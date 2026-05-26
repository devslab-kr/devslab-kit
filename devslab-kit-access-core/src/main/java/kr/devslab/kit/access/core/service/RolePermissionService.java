package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformRolePermissionEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformRolePermissionRepository;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import org.springframework.transaction.annotation.Transactional;

public class RolePermissionService {

    private final JpaPlatformRolePermissionRepository repository;
    private final Clock clock;

    public RolePermissionService(JpaPlatformRolePermissionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void grant(RoleId roleId, PermissionId permissionId) {
        if (repository.findByRoleIdAndPermissionId(roleId.value(), permissionId.value()).isPresent()) {
            return;
        }
        repository.save(new PlatformRolePermissionEntity(
                UUID.randomUUID(),
                roleId.value(),
                permissionId.value(),
                Instant.now(clock)
        ));
    }

    @Transactional
    public void revoke(RoleId roleId, PermissionId permissionId) {
        repository.deleteByRoleIdAndPermissionId(roleId.value(), permissionId.value());
    }

    @Transactional(readOnly = true)
    public List<PermissionId> findPermissionIdsForRole(RoleId roleId) {
        return repository.findAllByRoleId(roleId.value()).stream()
                .map(entity -> PermissionId.of(entity.getPermissionId()))
                .toList();
    }
}
