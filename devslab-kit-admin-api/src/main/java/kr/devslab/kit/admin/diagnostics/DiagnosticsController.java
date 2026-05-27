package kr.devslab.kit.admin.diagnostics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.LoginCommand;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.LoginResult;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import kr.devslab.kit.menu.MenuTree;
import kr.devslab.kit.menu.core.service.MenuTreeBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.BASE + "/diagnostics")
public class DiagnosticsController {

    private final LocalLoginService loginService;
    private final JpaPlatformUserAccountRepository userRepo;
    private final JpaPlatformPermissionRepository permissionRepo;
    private final kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository menuRepo;
    private final MenuTreeBuilder menuTreeBuilder;

    public DiagnosticsController(
            LocalLoginService loginService,
            JpaPlatformUserAccountRepository userRepo,
            JpaPlatformPermissionRepository permissionRepo,
            kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository menuRepo,
            MenuTreeBuilder menuTreeBuilder
    ) {
        this.loginService = loginService;
        this.userRepo = userRepo;
        this.permissionRepo = permissionRepo;
        this.menuRepo = menuRepo;
        this.menuTreeBuilder = menuTreeBuilder;
    }

    @PostMapping("/login-test")
    public LoginTestResponse loginTest(@Valid @RequestBody LoginTestRequest req) {
        LoginCommand cmd = new LoginCommand(TenantId.of(req.tenantId()), req.loginId(), req.rawPassword());
        try {
            LoginResult result = loginService.login(cmd);
            CurrentUser u = result.user();
            return new LoginTestResponse(true, null, u.id().value().toString(),
                    u.publicId().value(), u.loginId(), u.status().name());
        } catch (AccountLoginException ex) {
            return new LoginTestResponse(false, ex.reason(), null, null, null, null);
        }
    }

    @PostMapping("/permission-check")
    public PermissionCheckResponse permissionCheck(@Valid @RequestBody PermissionCheckRequest req) {
        Set<String> codes = permissionRepo.findCodesForUserId(req.userId());
        return new PermissionCheckResponse(codes.contains(req.permissionCode()), codes);
    }

    @GetMapping("/menu-visibility")
    public MenuTree menuVisibility(@RequestParam UUID userId) {
        PlatformUserAccountEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Set<String> codes = permissionRepo.findCodesForUserId(userId);
        var entities = menuRepo.findAllByTenantIdOrderBySortOrderAsc(user.getTenantId());
        MenuTree fullTree = menuTreeBuilder.build(entities);
        return new MenuTree(fullTree.roots().stream()
                .map(item -> filterByCodes(item, codes))
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    private kr.devslab.kit.menu.MenuItem filterByCodes(kr.devslab.kit.menu.MenuItem item, Set<String> codes) {
        if (item.requiredPermission().isPresent() && !codes.contains(item.requiredPermission().get().code())) {
            return null;
        }
        var visibleChildren = item.children().stream()
                .map(c -> filterByCodes(c, codes))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new kr.devslab.kit.menu.MenuItem(
                item.id(), item.code(), item.label(), item.path(), item.icon(),
                item.requiredPermission(), visibleChildren);
    }

    public record LoginTestRequest(
            @NotBlank String tenantId,
            @NotBlank String loginId,
            @NotBlank String rawPassword
    ) {
    }

    public record LoginTestResponse(
            boolean success,
            LoginFailureReason failureReason,
            String userId,
            String publicId,
            String loginId,
            String status
    ) {
    }

    public record PermissionCheckRequest(
            @NotNull UUID userId,
            @NotBlank String permissionCode
    ) {
    }

    public record PermissionCheckResponse(boolean allowed, Set<String> userPermissionCodes) {
    }
}
