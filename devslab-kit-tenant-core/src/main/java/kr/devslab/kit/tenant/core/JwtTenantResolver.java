package kr.devslab.kit.tenant.core;

import jakarta.servlet.http.HttpServletRequest;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.tenant.TenantResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the active tenant from the kit-issued bearer JWT: reads the
 * {@code Authorization: Bearer <token>} header, parses it with
 * {@link AuthTokenService}, and uses the token's tenant claim. Falls back to the
 * configured default tenant when there is no valid token — e.g. on the login
 * request itself, or an unauthenticated probe.
 *
 * <p>This reads the kit's own HS256 admin token (which already carries a
 * {@code tenant} claim). Validating <em>external</em> OAuth2 / OIDC tokens (JWKS,
 * issuer checks, a configurable claim name) is a separate, larger concern and is
 * intentionally out of scope here.
 */
public class JwtTenantResolver implements TenantResolver {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService authTokenService;
    private final String defaultTenantId;

    public JwtTenantResolver(AuthTokenService authTokenService, String defaultTenantId) {
        this.authTokenService = authTokenService;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public TenantContext resolve() {
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String header = request.getHeader(AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                String token = header.substring(BEARER_PREFIX.length()).trim();
                TenantContext fromToken = authTokenService.parse(token)
                        .map(user -> new TenantContext(user.tenantId()))
                        .orElse(null);
                if (fromToken != null) {
                    return fromToken;
                }
            }
        }
        if (defaultTenantId != null && !defaultTenantId.isBlank()) {
            return new TenantContext(TenantId.of(defaultTenantId));
        }
        throw new IllegalStateException(
                "No valid bearer token to resolve a tenant from, and no default tenant configured"
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
