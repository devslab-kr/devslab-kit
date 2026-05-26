package kr.devslab.kit.admin.menu;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.core.id.TenantId;
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
                req.sortOrder(),
                req.requiredPermissionCode()
        );
        return ResponseEntity.status(201).body(MenuResponse.from(entity));
    }

    @GetMapping
    public List<MenuResponse> list(@RequestParam String tenantId) {
        return service.listByTenant(TenantId.of(tenantId)).stream().map(MenuResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateMenuRequest req) {
        service.update(MenuId.of(id), req.label(), req.path(), req.sortOrder(), req.requiredPermissionCode());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(MenuId.of(id));
        return ResponseEntity.noContent().build();
    }
}
