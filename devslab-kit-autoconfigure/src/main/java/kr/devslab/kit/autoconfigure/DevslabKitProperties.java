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

    public Identity getIdentity() {
        return identity;
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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Audit {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
