package kr.devslab.kit.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates admin-API requests from a Bearer JWT and populates the Spring
 * Security context with the caller's roles <em>and effective permissions</em>, so
 * {@link AdminSecurityConfig}'s {@code hasAuthority("admin.*")} rules enforce real RBAC.
 *
 * <p>Permissions are resolved <strong>per request</strong> from the database
 * ({@link JpaPlatformPermissionRepository#findCodesForUserId} — the same effective-grant
 * query, spanning direct role grants and group→role grants, that {@code PermissionChecker}
 * uses) rather than read from the token. That keeps the JWT small and, more importantly,
 * makes a permission grant/revocation take effect on the very next request instead of
 * lingering until the token expires.
 *
 * <ul>
 *   <li>roles → {@code ROLE_<code>} authorities (for any role-based rules a consumer adds);</li>
 *   <li>permissions → the raw {@code admin.x.y} code as the authority, matched by
 *       {@code hasAuthority(...)} in the admin security chain.</li>
 * </ul>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService tokenService;
    private final JpaPlatformPermissionRepository permissionRepository;

    public JwtAuthenticationFilter(AuthTokenService tokenService,
                                   JpaPlatformPermissionRepository permissionRepository) {
        this.tokenService = tokenService;
        this.permissionRepository = permissionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            tokenService.parse(token).ifPresent(user -> authenticate(user, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(CurrentUser user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : user.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : permissionRepository.findCodesForUserId(user.id().value())) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
