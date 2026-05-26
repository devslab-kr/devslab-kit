package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.access.Permission;
import kr.devslab.kit.access.core.entity.PlatformPermissionEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.core.id.PermissionId;
import org.springframework.transaction.annotation.Transactional;

public class PermissionAdminService {

    private final JpaPlatformPermissionRepository repository;
    private final Clock clock;

    public PermissionAdminService(JpaPlatformPermissionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Permission create(String code, String description) {
        if (repository.findByCode(code).isPresent()) {
            throw new IllegalStateException("Permission already exists: code=" + code);
        }
        PlatformPermissionEntity entity = new PlatformPermissionEntity(UUID.randomUUID(), code, description, Instant.now(clock));
        repository.save(entity);
        return Permission.of(entity.getCode());
    }

    @Transactional
    public void updateDescription(PermissionId id, String newDescription) {
        PlatformPermissionEntity e = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));
        e.setDescription(newDescription);
    }

    @Transactional
    public void delete(PermissionId id) {
        repository.deleteById(id.value());
    }

    @Transactional(readOnly = true)
    public Optional<PlatformPermissionEntity> findById(PermissionId id) {
        return repository.findById(id.value());
    }

    @Transactional(readOnly = true)
    public List<PlatformPermissionEntity> listAll() {
        return repository.findAll();
    }
}
