package kr.devslab.kit.autoconfigure;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;

/**
 * Fail-fast guard for config sync (ADR 0003 §5). Config sync is a dev/staging convenience
 * and must never be enabled in production: production config is promoted via the
 * git-committed bundle applied on deploy, not an ad-hoc push.
 *
 * <p>If {@code devslab.kit.config-sync.enabled=true} while a production profile
 * ({@code prod}/{@code production}) is active, the application <strong>refuses to start</strong>
 * with a clear message — rather than silently disabling the feature, which would hide the
 * misconfiguration.
 *
 * <p>This guard is only instantiated when config sync is enabled (it is a bean of
 * {@link ConfigSyncAutoConfiguration}, which is itself gated on the enabled property), so
 * its presence already means enabled=true; it only has to inspect the active profiles.
 */
public class ConfigSyncProductionGuard implements InitializingBean {

    private static final Set<String> PRODUCTION_PROFILES = Set.of("prod", "production");

    private final Environment environment;

    public ConfigSyncProductionGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String productionProfile = Arrays.stream(environment.getActiveProfiles())
                .filter(PRODUCTION_PROFILES::contains)
                .findFirst()
                .orElse(null);
        if (productionProfile != null) {
            throw new IllegalStateException(
                    "devslab.kit.config-sync.enabled=true while the '" + productionProfile
                            + "' profile is active. Config sync is a dev/staging-only tool and must not run in "
                            + "production. Remove the property from the production configuration (promote config to "
                            + "production via the git-committed bundle on deploy instead), or run without a "
                            + "production profile.");
        }
    }
}
