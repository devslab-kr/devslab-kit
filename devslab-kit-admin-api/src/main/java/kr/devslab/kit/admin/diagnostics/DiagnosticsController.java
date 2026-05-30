package kr.devslab.kit.admin.diagnostics;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.access.PermissionGrant;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.service.PermissionGrantQueryService;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.LoginCommand;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.LoginResult;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import kr.devslab.kit.menu.MenuItem;
import kr.devslab.kit.menu.MenuTree;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
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
    private final PermissionGrantQueryService grantQueryService;
    private final JpaPlatformMenuRepository menuRepo;
    private final MenuTreeBuilder menuTreeBuilder;

    public DiagnosticsController(
            LocalLoginService loginService,
            JpaPlatformUserAccountRepository userRepo,
            JpaPlatformPermissionRepository permissionRepo,
            PermissionGrantQueryService grantQueryService,
            JpaPlatformMenuRepository menuRepo,
            MenuTreeBuilder menuTreeBuilder
    ) {
        this.loginService = loginService;
        this.userRepo = userRepo;
        this.permissionRepo = permissionRepo;
        this.grantQueryService = grantQueryService;
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
        boolean has = codes.contains(req.permissionCode());
        // Resolve every grant path the user has on this permission so the UI
        // can show "you got this via role:ADMIN" or "via group:eng-team>role:READ".
        // Empty when has == false; can be multi-row when the user holds the
        // permission both directly and through a group.
        List<String> matchedVia = has
                ? grantQueryService.findGrantsFor(req.userId(), req.permissionCode()).stream()
                        .map(PermissionGrant::describe)
                        .distinct()
                        .toList()
                : List.of();
        return new PermissionCheckResponse(has, matchedVia);
    }

    /**
     * Return the per-tenant menu tree marked with a {@code visible} flag
     * per node rather than pre-pruning hidden items. The admin UI uses the
     * flag to render greyed-out items for parent / sibling context.
     */
    @GetMapping("/menu-visibility")
    public MenuVisibilityResponse menuVisibility(@RequestParam UUID userId) {
        PlatformUserAccountEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Set<String> codes = permissionRepo.findCodesForUserId(userId);
        var entities = menuRepo.findAllByTenantIdOrderBySortOrderAsc(user.getTenantId());
        MenuTree fullTree = menuTreeBuilder.build(entities);
        List<MenuVisibilityItem> items = fullTree.roots().stream()
                .map(item -> annotate(item, codes))
                .filter(Objects::nonNull)
                .toList();
        return new MenuVisibilityResponse(items);
    }

    private MenuVisibilityItem annotate(MenuItem item, Set<String> codes) {
        boolean visible = item.requiredPermission().isEmpty()
                || codes.contains(item.requiredPermission().get().code());
        List<MenuVisibilityItem> children = item.children().stream()
                .map(c -> annotate(c, codes))
                .filter(Objects::nonNull)
                .toList();
        return new MenuVisibilityItem(
                item.id().value().toString(),
                item.code(),
                item.label(),
                visible,
                item.requiredPermission().map(p -> p.code()).orElse(null),
                children
        );
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

    /**
     * Wire shape mirrors the admin UI's {@code PermissionCheckResponse}:
     * {@code hasPermission} + {@code matchedVia} (the path that granted it).
     */
    public record PermissionCheckResponse(boolean hasPermission, List<String> matchedVia) {
    }

    /**
     * Wire shape for {@code GET /diagnostics/menu-visibility}. The admin UI
     * reads {@code items} (not {@code roots}) and uses {@code visible} to
     * render greyed-out nodes for context.
     */
    public record MenuVisibilityResponse(List<MenuVisibilityItem> items) {
    }

    public record MenuVisibilityItem(
            String id,
            String code,
            String label,
            boolean visible,
            String requiredPermission,
            List<MenuVisibilityItem> children
    ) {
    }
}
