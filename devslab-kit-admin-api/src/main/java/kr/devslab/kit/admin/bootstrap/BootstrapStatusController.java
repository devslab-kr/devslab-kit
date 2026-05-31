package kr.devslab.kit.admin.bootstrap;

import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reports whether the platform has been provisioned yet (ADR 0001).
 *
 * <p>Deliberately <strong>unauthenticated</strong> — a guided first-run / setup
 * wizard has to ask this <em>before</em> any account exists, i.e. before anyone
 * could possibly hold a token. It leaks only a single boolean (does at least one
 * account exist), never any account detail, so exposing it pre-auth is safe and
 * is whitelisted in {@code AdminSecurityConfig}.
 */
@RestController
@RequestMapping(AdminApiPaths.BASE + "/bootstrap")
public class BootstrapStatusController {

    private final JpaPlatformUserAccountRepository userRepository;

    public BootstrapStatusController(JpaPlatformUserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public BootstrapStatusResponse status() {
        return new BootstrapStatusResponse(userRepository.count() > 0);
    }
}
