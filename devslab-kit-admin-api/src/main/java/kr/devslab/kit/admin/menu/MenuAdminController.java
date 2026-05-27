package kr.devslab.kit.admin.menu;

import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.MENUS)
public class MenuAdminController {

    private final MenuAdminService service;

    public MenuAdminController(MenuAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(@Valid @RequestBody CreateMenuRequest req) {
        var entity = service.create(
                TenantId.of(req.tenantId()),
                req.code(),
                req.label(),
                req.path(),
                req.parentId() == null ? null : MenuId.of(req.parentId()),
                req.displayOrder(),
                req.requiredPermission(),
                req.icon()
        );
        return ResponseEntity.status(201).body(MenuResponse.flat(entity));
    }

    @GetMapping
    public List<MenuResponse> list(@RequestParam String tenantId) {
        return service.listByTenant(TenantId.of(tenantId)).stream().map(MenuResponse::flat).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> get(@PathVariable UUID id) {
        return service.findById(MenuId.of(id))
                .map(MenuResponse::flat)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Tree view for the admin UI's TreeTable component. Returns root nodes only;
     * each root carries its descendants nested under {@code children}, sorted by
     * {@code displayOrder}.
     */
    @GetMapping("/tree")
    public List<MenuResponse> tree(@RequestParam String tenantId) {
        List<PlatformMenuEntity> entities = service.listByTenant(TenantId.of(tenantId));
        Map<UUID, List<PlatformMenuEntity>> byParent = entities.stream()
                .filter(e -> e.getParentId() != null)
                .collect(Collectors.groupingBy(PlatformMenuEntity::getParentId));
        return entities.stream()
                .filter(e -> e.getParentId() == null)
                .sorted(Comparator.comparingInt(PlatformMenuEntity::getSortOrder))
                .map(root -> toNode(root, byParent))
                .toList();
    }

    private MenuResponse toNode(PlatformMenuEntity entity, Map<UUID, List<PlatformMenuEntity>> byParent) {
        List<MenuResponse> children = byParent.getOrDefault(entity.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(PlatformMenuEntity::getSortOrder))
                .map(child -> toNode(child, byParent))
                .toList();
        return MenuResponse.withChildren(entity, children);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateMenuRequest req) {
        service.update(
                MenuId.of(id),
                req.label(),
                req.path(),
                req.displayOrder(),
                req.requiredPermission(),
                req.icon()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(MenuId.of(id));
        return ResponseEntity.noContent().build();
    }
}
