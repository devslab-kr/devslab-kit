package kr.devslab.kit.tenant.core;

import jakarta.servlet.http.HttpServletRequest;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.tenant.TenantResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class HeaderTenantResolver implements TenantResolver {

    public static final String DEFAULT_HEADER = "X-Tenant-Id";

    private final String headerName;
    private final String defaultTenantId;

    public HeaderTenantResolver(String headerName, String defaultTenantId) {
        this.headerName = headerName == null ? DEFAULT_HEADER : headerName;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public TenantContext resolve() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return new TenantContext(TenantId.of(value.trim()));
            }
        }
        if (defaultTenantId != null && !defaultTenantId.isBlank()) {
            return new TenantContext(TenantId.of(defaultTenantId));
        }
        throw new IllegalStateException(
                "Tenant header '" + headerName + "' missing and no default tenant configured"
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
