package kr.devslab.kit.admin.auth;

import jakarta.validation.Valid;
import java.util.Set;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.LoginCommand;
import kr.devslab.kit.identity.LoginResult;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.BASE + "/auth")
public class AuthController {

    private final LocalLoginService loginService;
    private final AuthTokenService tokenService;
    private final JpaPlatformRoleRepository roleRepository;

    public AuthController(
            LocalLoginService loginService,
            AuthTokenService tokenService,
            JpaPlatformRoleRepository roleRepository
    ) {
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResult result = loginService.login(new LoginCommand(
                TenantId.of(req.tenantId()),
                req.loginId(),
                req.rawPassword()
        ));
        // LocalLoginService leaves CurrentUser.roles empty because it lives in
        // identity-core, which deliberately doesn't depend on access-core.
        // Enrich here — admin-api already depends on both — so the JWT claims
        // and the LoginResponse carry the user's effective role set.
        CurrentUser base = result.user();
        Set<String> roleCodes = roleRepository.findRoleCodesForUserId(base.id().value());
        CurrentUser enriched = new CurrentUser(
                base.id(),
                base.publicId(),
                base.tenantId(),
                base.loginId(),
                base.status(),
                roleCodes
        );
        AuthToken token = tokenService.issue(enriched);
        return ResponseEntity.ok(LoginResponse.of(token, enriched));
    }
}
