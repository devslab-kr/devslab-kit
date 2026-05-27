package kr.devslab.kit.tenant.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantMetadata;
import kr.devslab.kit.tenant.TenantMode;
import kr.devslab.kit.tenant.TenantService;
import kr.devslab.kit.tenant.TenantStatus;
import kr.devslab.kit.tenant.core.entity.PlatformTenantEntity;
import kr.devslab.kit.tenant.core.repository.JpaPlatformTenantRepository;
import org.springframework.transaction.annotation.Transactional;

public class DefaultTenantService implements TenantService {

    private final JpaPlatformTenantRepository repository;
    private final Clock clock;

    public DefaultTenantService(JpaPlatformTenantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TenantMetadata create(TenantId id, String name, TenantMode mode) {
        if (repository.findById(id.value()).isPresent()) {
            throw new IllegalStateException("Tenant already exists: " + id);
        }
        PlatformTenantEntity entity = new PlatformTenantEntity(
                id.value(), name, mode, TenantStatus.ACTIVE, Instant.now(clock));
        repository.save(entity);
        return toMetadata(entity);
    }

    @Override
    @Transactional
    public void rename(TenantId id, String newName) {
        PlatformTenantEntity entity = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        entity.setName(newName);
    }

    @Override
    @Transactional
    public void setStatus(TenantId id, TenantStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        PlatformTenantEntity entity = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        entity.setStatus(status);
    }

    @Override
    @Transactional
    public void delete(TenantId id) {
        repository.deleteById(id.value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantMetadata> findById(TenantId id) {
        return repository.findById(id.value()).map(this::toMetadata);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMetadata> findAll() {
        return repository.findAll().stream().map(this::toMetadata).toList();
    }

    private TenantMetadata toMetadata(PlatformTenantEntity e) {
        return new TenantMetadata(
                TenantId.of(e.getId()),
                e.getName(),
                e.getMode(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }
}
