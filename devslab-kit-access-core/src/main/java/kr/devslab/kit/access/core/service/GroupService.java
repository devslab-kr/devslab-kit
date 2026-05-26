package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.Group;
import kr.devslab.kit.access.core.entity.PlatformGroupEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformGroupRepository;
import kr.devslab.kit.core.id.GroupId;
import kr.devslab.kit.core.id.TenantId;
import org.springframework.transaction.annotation.Transactional;

public class GroupService {

    private final JpaPlatformGroupRepository repository;
    private final Clock clock;

    public GroupService(JpaPlatformGroupRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Group create(TenantId tenantId, String code, String name, GroupId parentGroupId) {
        if (repository.findByTenantIdAndCode(tenantId.value(), code).isPresent()) {
            throw new IllegalStateException("Group already exists: tenant=" + tenantId + " code=" + code);
        }
        PlatformGroupEntity entity = new PlatformGroupEntity(
                UUID.randomUUID(),
                tenantId.value(),
                code,
                name,
                parentGroupId == null ? null : parentGroupId.value(),
                Instant.now(clock)
        );
        repository.save(entity);
        return toModel(entity);
    }

    @Transactional
    public void rename(GroupId id, String newName) {
        PlatformGroupEntity entity = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
        entity.setName(newName);
    }

    @Transactional
    public void delete(GroupId id) {
        repository.deleteById(id.value());
    }

    @Transactional(readOnly = true)
    public Optional<Group> findByTenantAndCode(TenantId tenantId, String code) {
        return repository.findByTenantIdAndCode(tenantId.value(), code).map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<Group> findAllByTenant(TenantId tenantId) {
        return repository.findAllByTenantId(tenantId.value()).stream().map(this::toModel).toList();
    }

    private Group toModel(PlatformGroupEntity entity) {
        return new Group(
                GroupId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                entity.getCode(),
                entity.getName(),
                Optional.ofNullable(entity.getParentGroupId()).map(GroupId::of)
        );
    }
}
