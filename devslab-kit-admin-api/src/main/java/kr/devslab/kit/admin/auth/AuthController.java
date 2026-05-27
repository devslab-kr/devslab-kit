package kr.devslab.kit.admin.auth;

import jakarta.validation.Valid;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.AuthTokenService;
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

    public AuthController(LocalLoginService loginService, AuthTokenService tokenService) {
        this.loginService = loginService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResult result = loginService.login(new LoginCommand(
                TenantId.of(req.tenantId()),
                req.loginId(),
                req.rawPassword()
        ));
        AuthToken token = tokenService.issue(result.user());
        return ResponseEntity.ok(LoginResponse.of(token, result.user()));
    }
}
