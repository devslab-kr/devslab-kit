package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.tenant.TenantMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devslab.kit")
@Getter
public class DevslabKitProperties {

    private final Identity identity = new Identity();
    private final Access access = new Access();
    private final Tenant tenant = new Tenant();
    private final Menu menu = new Menu();
    private final Audit audit = new Audit();
    private final Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Identity {

        private boolean enabled = true;
        private Mode mode = Mode.LOCAL;
        private int maxFailedAttempts = 5;
        private java.time.Duration lockoutDuration = java.time.Duration.ofMinutes(15);
        private final Jwt jwt = new Jwt();

        @Getter
        @Setter
        public static class Jwt {
            private String secret = "change-me-please-this-is-not-a-real-secret-32b!";
            private java.time.Duration ttl = java.time.Duration.ofHours(8);
            private String issuer = "devslab-kit";
        }

        public enum Mode {
            LOCAL,
            EXTERNAL_JWT,
            HYBRID,
            CUSTOM
        }
    }

    @Getter
    @Setter
    public static class Access {

        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Tenant {

        private boolean enabled = true;
        private TenantMode mode = TenantMode.SINGLE;
        private String defaultTenantId = "default";
        private Resolver resolver = Resolver.FIXED;
        private String headerName = "X-Tenant-Id";
        private int subdomainIndex = 0;

        public enum Resolver {
            FIXED,
            HEADER,
            JWT,
            SUBDOMAIN,
            CUSTOM
        }
    }

    @Getter
    @Setter
    public static class Menu {

        private boolean enabled = true;

        /**
         * How long a {@link kr.devslab.kit.menu.MenuProvider} keeps a
         * per-user menu tree before re-querying the database. Set to
         * {@code PT0S} (or any zero / negative duration) to disable the
         * cache entirely — useful in tests where menu / permission
         * mutations should be visible immediately.
         */
        private java.time.Duration cacheTtl = java.time.Duration.ofMinutes(5);
    }

    @Getter
    @Setter
    public static class Audit {

        private boolean enabled = true;

        /**
         * Bounded capacity of the per-publisher work queue that
         * {@link kr.devslab.kit.audit.core.service.DefaultAuditEventPublisher}
         * uses to write events without blocking the request thread. When the
         * queue is saturated the publisher falls back to running the write
         * inline on the caller (CallerRunsPolicy) so events are never lost,
         * trading a slow request for a dropped audit row.
         */
        private int asyncQueueCapacity = 1024;
    }

    @Getter
    @Setter
    public static class Bootstrap {

        /** Master switch. Default OFF — bootstrap runs only when explicitly enabled. */
        private boolean enabled = false;

        /** Tenant the first admin belongs to; created if absent. */
        private String tenantId = "default";

        private String adminLoginId = "admin";

        /**
         * Admin password. Leave blank to generate a strong random one and log
         * it exactly once on first boot (GitLab / Jenkins style). A fixed value
         * only ever exists because an operator wrote it here.
         */
        private String adminPassword;

        private String adminEmail = "admin@example.com";

        /** Code of the role granted to the bootstrap admin; created if absent. */
        private String roleCode = "PLATFORM_ADMIN";

        /** Display name used when the role has to be created. */
        private String roleName = "Platform Admin";

        /**
         * When true (default), the bootstrap admin must rotate its password on
         * first login. Local-dev typically sets this to {@code false} to skip
         * the extra round-trip.
         */
        private boolean mustChangePassword = true;

        /**
         * Backstop, not the primary control: if the active profiles include
         * {@code prod}/{@code production} and an explicitly-configured password
         * is a well-known weak value ({@code admin}, {@code password}, …), fail
         * startup. The primary control is that no fixed default exists at all.
         */
        private boolean failOnDefaultPasswordInProd = true;
    }
}
