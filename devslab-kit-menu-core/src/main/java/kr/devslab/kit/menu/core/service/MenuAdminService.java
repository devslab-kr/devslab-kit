package kr.devslab.kit.menu.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
import org.springframework.transaction.annotation.Transactional;

public class MenuAdminService {

    private final JpaPlatformMenuRepository repository;
    private final Clock clock;

    public MenuAdminService(JpaPlatformMenuRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public PlatformMenuEntity create(
            TenantId tenantId,
            String code,
            String label,
            String path,
            MenuId parentId,
            int sortOrder,
            String requiredPermissionCode
    ) {
        PlatformMenuEntity entity = new PlatformMenuEntity(
                UUID.randomUUID(),
                tenantId.value(),
                code,
                label,
                path,
                parentId == null ? null : parentId.value(),
                sortOrder,
                requiredPermissionCode,
                Instant.now(clock)
        );
        repository.save(entity);
        return entity;
    }

    @Transactional
    public void update(MenuId id, String label, String path, Integer sortOrder, String requiredPermissionCode) {
        PlatformMenuEntity e = repository.findById(id.value())
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + id));
        if (label != null) e.setLabel(label);
        if (path != null) e.setPath(path);
        if (sortOrder != null) e.setSortOrder(sortOrder);
        if (requiredPermissionCode != null) e.setRequiredPermissionCode(requiredPermissionCode);
    }

    @Transactional
    public void delete(MenuId id) {
        repository.deleteById(id.value());
    }

    @Transactional(readOnly = true)
    public List<PlatformMenuEntity> listByTenant(TenantId tenantId) {
        return repository.findAllByTenantIdOrderBySortOrderAsc(tenantId.value());
    }
}
