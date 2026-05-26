package kr.devslab.kit.admin.tenant;

import jakarta.validation.Valid;
import java.util.List;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantMetadata;
import kr.devslab.kit.tenant.TenantService;
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
@RequestMapping(AdminApiPaths.BASE + "/tenants")
public class TenantAdminController {

    private final TenantService tenantService;

    public TenantAdminController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantMetadata> create(@Valid @RequestBody CreateTenantRequest req) {
        TenantMetadata created = tenantService.create(TenantId.of(req.id()), req.name(), req.mode());
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    public List<TenantMetadata> list() {
        return tenantService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantMetadata> get(@PathVariable String id) {
        return tenantService.findById(TenantId.of(id))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> rename(@PathVariable String id, @Valid @RequestBody RenameTenantRequest req) {
        tenantService.rename(TenantId.of(id), req.name());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable String id) {
        tenantService.activate(TenantId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        tenantService.deactivate(TenantId.of(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tenantService.delete(TenantId.of(id));
        return ResponseEntity.noContent().build();
    }
}
