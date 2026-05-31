package kr.devslab.kit.admin.auth;

import jakarta.validation.Valid;
import java.util.Set;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.CurrentUserProvider;
import kr.devslab.kit.identity.LoginCommand;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.LoginResult;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import kr.devslab.kit.identity.core.service.SelfServicePasswordService;
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
    private final CurrentUserProvider currentUserProvider;
    private final SelfServicePasswordService passwordService;

    public AuthController(
            LocalLoginService loginService,
            AuthTokenService tokenService,
            JpaPlatformRoleRepository roleRepository,
            CurrentUserProvider currentUserProvider,
            SelfServicePasswordService passwordService
    ) {
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordService = passwordService;
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
                roleCodes,
                base.mustChangePassword()
        );
        AuthToken token = tokenService.issue(enriched);
        return ResponseEntity.ok(LoginResponse.of(token, enriched));
    }

    /**
     * Self-service password change for the authenticated caller. Verifies the
     * current password, stores the new one, and clears the forced-rotation
     * flag — this is how a bootstrap admin (ADR 0001) escapes the
     * {@code mustChangePassword} gate. Returns a freshly minted token (now with
     * {@code mustChangePassword=false}) so the dashboard can swap it in without
     * a re-login round-trip.
     */
    @PostMapping("/change-password")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        CurrentUser principal = currentUserProvider.current()
                .orElseThrow(() -> new AccountLoginException(LoginFailureReason.UNKNOWN_USER));

        passwordService.changePassword(principal.id(), req.oldPassword(), req.newPassword());

        // Re-issue with the flag cleared, re-reading roles so the new token is
        // consistent with login's response shape.
        Set<String> roleCodes = roleRepository.findRoleCodesForUserId(principal.id().value());
        CurrentUser updated = new CurrentUser(
                principal.id(),
                principal.publicId(),
                principal.tenantId(),
                principal.loginId(),
                principal.status(),
                roleCodes,
                false
        );
        AuthToken token = tokenService.issue(updated);
        return ResponseEntity.ok(LoginResponse.of(token, updated));
    }
}
