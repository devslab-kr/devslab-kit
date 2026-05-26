package kr.devslab.kit.tenant.core;

import jakarta.servlet.http.HttpServletRequest;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.tenant.TenantResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SubdomainTenantResolver implements TenantResolver {

    private final int subdomainIndex;
    private final String defaultTenantId;

    public SubdomainTenantResolver(int subdomainIndex, String defaultTenantId) {
        if (subdomainIndex < 0) {
            throw new IllegalArgumentException("subdomainIndex must be >= 0");
        }
        this.subdomainIndex = subdomainIndex;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public TenantContext resolve() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String host = request.getServerName();
            if (host != null) {
                String[] parts = host.split("\\.");
                if (parts.length > subdomainIndex + 1) {
                    String segment = parts[subdomainIndex];
                    if (!segment.isBlank()) {
                        return new TenantContext(TenantId.of(segment));
                    }
                }
            }
        }
        if (defaultTenantId != null && !defaultTenantId.isBlank()) {
            return new TenantContext(TenantId.of(defaultTenantId));
        }
        throw new IllegalStateException(
                "Could not extract tenant from subdomain index " + subdomainIndex
                        + " and no default tenant configured"
        );
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
