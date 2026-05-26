package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformGroupRoleEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformGroupRoleRepository;
import kr.devslab.kit.core.id.GroupId;
import kr.devslab.kit.core.id.RoleId;
import org.springframework.transaction.annotation.Transactional;

public class GroupRoleService {

    private final JpaPlatformGroupRoleRepository repository;
    private final Clock clock;

    public GroupRoleService(JpaPlatformGroupRoleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void assign(GroupId groupId, RoleId roleId) {
        if (repository.findByGroupIdAndRoleId(groupId.value(), roleId.value()).isPresent()) {
            return;
        }
        repository.save(new PlatformGroupRoleEntity(
                UUID.randomUUID(),
                groupId.value(),
                roleId.value(),
                Instant.now(clock)
        ));
    }

    @Transactional
    public void revoke(GroupId groupId, RoleId roleId) {
        repository.deleteByGroupIdAndRoleId(groupId.value(), roleId.value());
    }

    @Transactional(readOnly = true)
    public List<RoleId> findRoleIdsForGroup(GroupId groupId) {
        return repository.findAllByGroupId(groupId.value()).stream()
                .map(entity -> RoleId.of(entity.getRoleId()))
                .toList();
    }
}
