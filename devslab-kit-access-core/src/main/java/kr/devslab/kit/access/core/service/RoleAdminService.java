package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.Role;
import kr.devslab.kit.access.core.entity.PlatformRoleEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import org.springframework.transaction.annotation.Transactional;

public class RoleAdminService {

    private final JpaPlatformRoleRepository repository;
    private final Clock clock;

    public RoleAdminService(JpaPlatformRoleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Role create(TenantId tenantId, String code, String name) {
        if (repository.findByTenantIdAndCode(tenantId.value(), code).isPresent()) {
            throw new IllegalStateException("Role already exists: tenant=" + tenantId + " code=" + code);
        }
        PlatformRoleEntity entity = new PlatformRoleEntity(UUID.randomUUID(), tenantId.value(), code, name, Instant.now(clock));
        repository.save(entity);
        return toModel(entity);
    }

    @Transactional
    public void rename(RoleId id, String newName) {
        PlatformRoleEntity e = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
        e.setName(newName);
    }

    @Transactional
    public void delete(RoleId id) {
        repository.deleteById(id.value());
    }

    @Transactional(readOnly = true)
    public Optional<Role> findById(RoleId id) {
        return repository.findById(id.value()).map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<Role> listByTenant(TenantId tenantId) {
        return repository.findAllByTenantId(tenantId.value()).stream().map(this::toModel).toList();
    }

    private Role toModel(PlatformRoleEntity entity) {
        return new Role(RoleId.of(entity.getId()), entity.getCode(), entity.getName());
    }
}
