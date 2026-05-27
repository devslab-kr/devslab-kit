package kr.devslab.kit.admin.settings;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import kr.devslab.kit.admin.AdminApiPaths;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of {@code devslab.kit.*} runtime settings.
 *
 * <p>Wire shape matches the admin UI's {@code AdminSettings} interface —
 * grouped by domain ({@code jwt}, {@code tenant}, {@code identity},
 * {@code audit}, {@code menu}) plus a {@code raw} key/value table of every
 * {@code devslab.*} property the runtime can see.
 *
 * <p>Sensitive fields (JWT secret, anything with {@code password} or
 * {@code secret} in the key) are masked in both the structured view and
 * the raw dump.
 *
 * <p>Mutating settings at runtime requires Spring Cloud RefreshScope or
 * an external config source — deliberately out of scope here.
 *
 * <p>Implemented against {@link Environment} (rather than
 * {@code DevslabKitProperties}) so this module avoids depending on
 * {@code devslab-kit-autoconfigure} and we keep the dependency graph
 * acyclic.
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
        root.put("jwt", jwtView());
        root.put("tenant", tenantView());
        root.put("identity", identityView());
        root.put("audit", auditView());
        root.put("menu", menuView());
        root.put("raw", rawDump());
        return root;
    }

    private Map<String, Object> jwtView() {
        Map<String, Object> jwt = new LinkedHashMap<>();
        jwt.put("issuer", env.getProperty(PREFIX + "identity.jwt.issuer", "devslab-kit"));
        jwt.put("ttlSeconds", durationToSeconds(env.getProperty(PREFIX + "identity.jwt.ttl"), 28800L));
        String secret = env.getProperty(PREFIX + "identity.jwt.secret");
        jwt.put("secretMasked", secret == null || secret.isBlank() ? null : MASKED);
        return jwt;
    }

    private Map<String, Object> tenantView() {
        Map<String, Object> tenant = new LinkedHashMap<>();
        tenant.put("resolver", env.getProperty(PREFIX + "tenant.resolver", "FIXED"));
        return tenant;
    }

    private Map<String, Object> identityView() {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("lockoutThreshold",
                env.getProperty(PREFIX + "identity.max-failed-attempts", Integer.class, 5));
        identity.put("lockoutDurationSeconds",
                durationToSeconds(env.getProperty(PREFIX + "identity.lockout-duration"), 900L));
        return identity;
    }

    private Map<String, Object> auditView() {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("enabled", env.getProperty(PREFIX + "audit.enabled", Boolean.class, true));
        audit.put("asyncQueueCapacity",
                env.getProperty(PREFIX + "audit.async-queue-capacity", Integer.class, 1024));
        return audit;
    }

    private Map<String, Object> menuView() {
        Map<String, Object> menu = new LinkedHashMap<>();
        menu.put("cacheTtlSeconds",
                durationToSeconds(env.getProperty(PREFIX + "menu.cache-ttl"), 300L));
        return menu;
    }

    /**
     * Dump every {@code devslab.*} entry the Environment can enumerate, with
     * secrets / passwords masked. Sorted for stable diffs in the UI.
     */
    private Map<String, String> rawDump() {
        Map<String, String> raw = new TreeMap<>();
        if (!(env instanceof ConfigurableEnvironment configurable)) {
            return raw;
        }
        for (PropertySource<?> source : configurable.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    if (!name.startsWith("devslab.") || raw.containsKey(name)) {
                        continue;
                    }
                    String value = env.getProperty(name);
                    if (value == null) {
                        continue;
                    }
                    raw.put(name, isSensitive(name) ? MASKED : value);
                }
            }
        }
        return raw;
    }

    private static boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return lower.contains("secret") || lower.contains("password") || lower.contains("token");
    }

    private static long durationToSeconds(String raw, long defaultSeconds) {
        if (raw == null || raw.isBlank()) {
            return defaultSeconds;
        }
        try {
            return Duration.parse(raw).toSeconds();
        } catch (Exception e) {
            return defaultSeconds;
        }
    }
}
