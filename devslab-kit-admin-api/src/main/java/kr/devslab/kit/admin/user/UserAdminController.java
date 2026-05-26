package kr.devslab.kit.admin.user;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.UserAccountView;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.identity.core.service.PlatformUserAccountService;
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
@RequestMapping(AdminApiPaths.USERS)
public class UserAdminController {

    private final PlatformUserAccountAdminService adminService;
    private final PlatformUserAccountService readService;

    public UserAdminController(
            PlatformUserAccountAdminService adminService,
            PlatformUserAccountService readService
    ) {
        this.adminService = adminService;
        this.readService = readService;
    }

    @PostMapping
    public ResponseEntity<UserAccountView> create(@Valid @RequestBody CreateUserRequest req) {
        UserAccountView view = adminService.create(
                TenantId.of(req.tenantId()),
                req.loginId(),
                req.email(),
                req.rawPassword(),
                req.providerType()
        );
        return ResponseEntity.status(201).body(view);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountView> get(@PathVariable UUID id) {
        return readService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<UserAccountView> list(@RequestParam String tenantId) {
        return adminService.listByTenant(TenantId.of(tenantId));
    }

    @PutMapping("/{id}/lock")
    public ResponseEntity<Void> lock(@PathVariable UUID id) {
        adminService.lock(UserId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable UUID id) {
        adminService.unlock(UserId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest req) {
        adminService.setStatus(UserId.of(id), req.status());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest req) {
        adminService.resetPassword(UserId.of(id), req.newRawPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminService.delete(UserId.of(id));
        return ResponseEntity.noContent().build();
    }
}
