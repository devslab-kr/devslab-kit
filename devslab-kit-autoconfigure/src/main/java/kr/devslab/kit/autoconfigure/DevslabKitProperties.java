package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.tenant.TenantMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devslab.kit")
public class DevslabKitProperties {

    private final Identity identity = new Identity();
    private final Access access = new Access();
    private final Tenant tenant = new Tenant();
    private final Menu menu = new Menu();
    private final Audit audit = new Audit();
    private final Bootstrap bootstrap = new Bootstrap();

    public Identity getIdentity() {
        return identity;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Access getAccess() {
        return access;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Menu getMenu() {
        return menu;
    }

    public Audit getAudit() {
        return audit;
    }

    public static class Identity {

        private boolean enabled = true;
        private Mode mode = Mode.LOCAL;
        private int maxFailedAttempts = 5;
        private java.time.Duration lockoutDuration = java.time.Duration.ofMinutes(15);
        private final Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public static class Jwt {
            private String secret = "change-me-please-this-is-not-a-real-secret-32b!";
            private java.time.Duration ttl = java.time.Duration.ofHours(8);
            private String issuer = "devslab-kit";

            public String getSecret() { return secret; }
            public void setSecret(String secret) { this.secret = secret; }
            public java.time.Duration getTtl() { return ttl; }
            public void setTtl(java.time.Duration ttl) { this.ttl = ttl; }
            public String getIssuer() { return issuer; }
            public void setIssuer(String issuer) { this.issuer = issuer; }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public java.time.Duration getLockoutDuration() {
            return lockoutDuration;
        }

        public void setLockoutDuration(java.time.Duration lockoutDuration) {
            this.lockoutDuration = lockoutDuration;
        }

        public enum Mode {
            LOCAL,
            EXTERNAL_JWT,
            HYBRID,
            CUSTOM
        }
    }

    public static class Access {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Tenant {

        private boolean enabled = true;
        private TenantMode mode = TenantMode.SINGLE;
        private String defaultTenantId = "default";
        private Resolver resolver = Resolver.FIXED;
        private String headerName = "X-Tenant-Id";
        private int subdomainIndex = 0;

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public int getSubdomainIndex() {
            return subdomainIndex;
        }

        public void setSubdomainIndex(int subdomainIndex) {
            this.subdomainIndex = subdomainIndex;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public TenantMode getMode() {
            return mode;
        }

        public void setMode(TenantMode mode) {
            this.mode = mode;
        }

        public String getDefaultTenantId() {
            return defaultTenantId;
        }

        public void setDefaultTenantId(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId;
        }

        public Resolver getResolver() {
            return resolver;
        }

        public void setResolver(Resolver resolver) {
            this.resolver = resolver;
        }

        public enum Resolver {
            FIXED,
            HEADER,
            JWT,
            SUBDOMAIN,
            CUSTOM
        }
    }

    public static class Menu {

        private boolean enabled = true;

        // The menu cache no longer has its own TTL knob: the per-user menu tree
        // now rides the shared CacheManager (ADR 0002 §5), so its lifetime is
        // governed by devslab.kit.cache.ttl (Redis) / the cache backend. The
        // former devslab.kit.menu.cache-ttl property was removed in that change.

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getAsyncQueueCapacity() {
            return asyncQueueCapacity;
        }

        public void setAsyncQueueCapacity(int asyncQueueCapacity) {
            this.asyncQueueCapacity = asyncQueueCapacity;
        }
    }

    /**
     * First-admin bootstrap (ADR 0001). Disabled by default so a no-config
     * production deploy provisions nothing. When enabled, an idempotent runner
     * creates a tenant, an admin role with the full {@code admin.*} permission
     * set, and a single admin user on first boot — just enough to log in to the
     * dashboard and take over.
     *
     * <p>Drive it per environment from profile-specific config (e.g.
     * {@code application-local.yml} sets {@code admin-password: admin} with
     * {@code must-change-password: false}; staging/prod inject a secret and
     * leave the forced rotation on). Never commit a fixed password to the
     * shared {@code application.yml}.
     */
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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getAdminLoginId() {
            return adminLoginId;
        }

        public void setAdminLoginId(String adminLoginId) {
            this.adminLoginId = adminLoginId;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getRoleCode() {
            return roleCode;
        }

        public void setRoleCode(String roleCode) {
            this.roleCode = roleCode;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public boolean isMustChangePassword() {
            return mustChangePassword;
        }

        public void setMustChangePassword(boolean mustChangePassword) {
            this.mustChangePassword = mustChangePassword;
        }

        public boolean isFailOnDefaultPasswordInProd() {
            return failOnDefaultPasswordInProd;
        }

        public void setFailOnDefaultPasswordInProd(boolean failOnDefaultPasswordInProd) {
            this.failOnDefaultPasswordInProd = failOnDefaultPasswordInProd;
        }
    }
}
