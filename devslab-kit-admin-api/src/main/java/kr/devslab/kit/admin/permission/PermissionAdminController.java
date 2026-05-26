package kr.devslab.kit.admin.permission;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.PermissionId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.PERMISSIONS)
public class PermissionAdminController {

    private final PermissionAdminService service;

    public PermissionAdminController(PermissionAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest req) {
        var permission = service.create(req.code(), req.description());
        return service.listAll().stream()
                .filter(e -> e.getCode().equals(permission.code()))
                .findFirst()
                .map(PermissionResponse::from)
                .map(r -> ResponseEntity.status(201).body(r))
                .orElseGet(() -> ResponseEntity.status(201).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> get(@PathVariable UUID id) {
        return service.findById(PermissionId.of(id))
                .map(PermissionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PermissionResponse> list() {
        return service.listAll().stream().map(PermissionResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdatePermissionRequest req) {
        service.updateDescription(PermissionId.of(id), req.description());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(PermissionId.of(id));
        return ResponseEntity.noContent().build();
    }
}
