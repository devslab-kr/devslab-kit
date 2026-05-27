package kr.devslab.kit.admin.settings;

import java.util.LinkedHashMap;
import java.util.Map;
import kr.devslab.kit.admin.AdminApiPaths;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of {@code devslab.kit.*} runtime settings.
 *
 * <p>Sensitive fields (JWT secret, etc.) are masked. Mutating settings at
 * runtime requires Spring Cloud RefreshScope or an external config source —
 * deliberately out of scope here.
 *
 * <p>Implemented against {@link Environment} (rather than {@code DevslabKitProperties})
 * so this module avoids depending on {@code devslab-kit-autoconfigure} and we
 * keep the dependency graph acyclic.
 */
@RestController
@RequestMapping(AdminApiPaths.BASE + "/settings")
public class SettingsAdminController {

    private static final String MASKED = "***";
    private static final String PREFIX = "devslab.kit.";

    private final Environment env;

    public SettingsAdminController(Environment env) {
        this.env = env;
    }

    @GetMapping
    public Map<String, Object> get() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("identity", identityView());
        root.put("access", section("access.enabled"));
        root.put("tenant", tenantView());
        root.put("menu", section("menu.enabled"));
        root.put("audit", section("audit.enabled"));
        return root;
    }

    private Map<String, Object> identityView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("enabled", env.getProperty(PREFIX + "identity.enabled", Boolean.class, true));
        view.put("mode", env.getProperty(PREFIX + "identity.mode", "LOCAL"));
        view.put("maxFailedAttempts", env.getProperty(PREFIX + "identity.max-failed-attempts", Integer.class, 5));
        view.put("lockoutDuration", env.getProperty(PREFIX + "identity.lockout-duration", "PT15M"));
        Map<String, Object> jwt = new LinkedHashMap<>();
        jwt.put("issuer", env.getProperty(PREFIX + "identity.jwt.issuer", "devslab-kit"));
        jwt.put("ttl", env.getProperty(PREFIX + "identity.jwt.ttl", "PT8H"));
        String secret = env.getProperty(PREFIX + "identity.jwt.secret");
        jwt.put("secret", secret == null || secret.isBlank() ? null : MASKED);
        view.put("jwt", jwt);
        return view;
    }

    private Map<String, Object> tenantView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("enabled", env.getProperty(PREFIX + "tenant.enabled", Boolean.class, true));
        view.put("mode", env.getProperty(PREFIX + "tenant.mode", "SINGLE"));
        view.put("defaultTenantId", env.getProperty(PREFIX + "tenant.default-tenant-id", "default"));
        view.put("resolver", env.getProperty(PREFIX + "tenant.resolver", "FIXED"));
        view.put("headerName", env.getProperty(PREFIX + "tenant.header-name", "X-Tenant-Id"));
        view.put("subdomainIndex", env.getProperty(PREFIX + "tenant.subdomain-index", Integer.class, 0));
        return view;
    }

    private Map<String, Object> section(String enabledKey) {
        return Map.of("enabled", env.getProperty(PREFIX + enabledKey, Boolean.class, true));
    }
}
