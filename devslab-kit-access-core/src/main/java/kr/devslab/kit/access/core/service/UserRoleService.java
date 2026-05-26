package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformUserRoleEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformUserRoleRepository;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import org.springframework.transaction.annotation.Transactional;

public class UserRoleService {

    private final JpaPlatformUserRoleRepository repository;
    private final Clock clock;

    public UserRoleService(JpaPlatformUserRoleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void assign(UserId userId, RoleId roleId, TenantId tenantId) {
        if (repository.findByUserIdAndRoleId(userId.value(), roleId.value()).isPresent()) {
            return;
        }
        repository.save(new PlatformUserRoleEntity(
                UUID.randomUUID(),
                userId.value(),
                roleId.value(),
                tenantId.value(),
                Instant.now(clock)
        ));
    }

    @Transactional
    public void revoke(UserId userId, RoleId roleId) {
        repository.deleteByUserIdAndRoleId(userId.value(), roleId.value());
    }

    @Transactional(readOnly = true)
    public List<RoleId> findRoleIdsForUser(UserId userId) {
        return repository.findAllByUserId(userId.value()).stream()
                .map(entity -> RoleId.of(entity.getRoleId()))
                .toList();
    }
}
