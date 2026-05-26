package kr.devslab.kit.admin.role;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.Role;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
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
@RequestMapping(AdminApiPaths.ROLES)
public class RoleAdminController {

    private final RoleAdminService roleAdminService;
    private final RolePermissionService rolePermissionService;
    private final UserRoleService userRoleService;

    public RoleAdminController(
            RoleAdminService roleAdminService,
            RolePermissionService rolePermissionService,
            UserRoleService userRoleService
    ) {
        this.roleAdminService = roleAdminService;
        this.rolePermissionService = rolePermissionService;
        this.userRoleService = userRoleService;
    }

    @PostMapping
    public ResponseEntity<Role> create(@Valid @RequestBody CreateRoleRequest req) {
        Role role = roleAdminService.create(TenantId.of(req.tenantId()), req.code(), req.name());
        return ResponseEntity.status(201).body(role);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> get(@PathVariable UUID id) {
        return roleAdminService.findById(RoleId.of(id))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Role> list(@RequestParam String tenantId) {
        return roleAdminService.listByTenant(TenantId.of(tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> rename(@PathVariable UUID id, @Valid @RequestBody RenameRoleRequest req) {
        roleAdminService.rename(RoleId.of(id), req.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleAdminService.delete(RoleId.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    public List<PermissionId> listPermissions(@PathVariable UUID id) {
        return rolePermissionService.findPermissionIdsForRole(RoleId.of(id));
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<Void> grantPermission(@PathVariable UUID id, @PathVariable UUID permissionId) {
        rolePermissionService.grant(RoleId.of(id), PermissionId.of(permissionId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<Void> revokePermission(@PathVariable UUID id, @PathVariable UUID permissionId) {
        rolePermissionService.revoke(RoleId.of(id), PermissionId.of(permissionId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/users/{userId}")
    public ResponseEntity<Void> assignToUser(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam String tenantId
    ) {
        userRoleService.assign(UserId.of(userId), RoleId.of(id), TenantId.of(tenantId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<Void> revokeFromUser(@PathVariable UUID id, @PathVariable UUID userId) {
        userRoleService.revoke(UserId.of(userId), RoleId.of(id));
        return ResponseEntity.noContent().build();
    }
}
