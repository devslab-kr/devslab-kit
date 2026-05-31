package kr.devslab.kit.admin.security;

import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.admin.AdminApiPaths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Ships the admin REST API with real RBAC already wired: every {@code /admin/api/v1/**}
 * endpoint is mapped to the {@code admin.*} permission it requires, enforced by Spring
 * Security against the authorities the {@link JwtAuthenticationFilter} resolves for the
 * caller. A consumer adds the starter and gets an authorization-enforced admin API with no
 * configuration of their own; the first-admin bootstrap seeds all {@code admin.*} permissions
 * onto {@code PLATFORM_ADMIN}, so the seeded admin can use every endpoint immediately.
 *
 * <p>The whole authorization matrix lives here, in one auditable place, rather than scattered
 * as {@code @PreAuthorize} across the controllers. The chain is scoped to {@code /admin/api/v1/**}
 * and every bean is {@code @ConditionalOnMissingBean}, so a consumer can supply their own
 * {@code SecurityFilterChain} or {@code JwtAuthenticationFilter} to change any of it.
 *
 * <p>Read endpoints require {@code *.read}; mutating endpoints require {@code *.write}. The
 * read rules are method-scoped ({@code GET}) and declared before each resource's catch-all so
 * the catch-all only matches the mutating verbs. {@code POST /auth/login} and the
 * unauthenticated {@code GET bootstrap/status} are public (a setup wizard calls them before any
 * account exists); everything else under the base path (e.g. self-service change-password) just
 * needs authentication.
 */
@Configuration
public class AdminSecurityConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            kr.devslab.kit.identity.AuthTokenService tokenService,
            JpaPlatformPermissionRepository permissionRepository
    ) {
        return new JwtAuthenticationFilter(tokenService, permissionRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain devslabKitAdminSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {
        String base = AdminApiPaths.BASE;
        http
                .securityMatcher(base + "/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // Public: setup wizard + login, before any account/permission exists.
                        .requestMatchers(HttpMethod.POST, base + "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.BOOTSTRAP_STATUS).permitAll()
                        // Users
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.USERS, AdminApiPaths.USERS + "/**")
                                .hasAuthority("admin.user.read")
                        .requestMatchers(AdminApiPaths.USERS, AdminApiPaths.USERS + "/**")
                                .hasAuthority("admin.user.write")
                        // Roles (incl. permission grants + user assignments)
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.ROLES, AdminApiPaths.ROLES + "/**")
                                .hasAuthority("admin.role.read")
                        .requestMatchers(AdminApiPaths.ROLES, AdminApiPaths.ROLES + "/**")
                                .hasAuthority("admin.role.write")
                        // Permissions
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.PERMISSIONS, AdminApiPaths.PERMISSIONS + "/**")
                                .hasAuthority("admin.permission.read")
                        .requestMatchers(AdminApiPaths.PERMISSIONS, AdminApiPaths.PERMISSIONS + "/**")
                                .hasAuthority("admin.permission.write")
                        // Groups (incl. memberships + role assignments)
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.GROUPS, AdminApiPaths.GROUPS + "/**")
                                .hasAuthority("admin.group.read")
                        .requestMatchers(AdminApiPaths.GROUPS, AdminApiPaths.GROUPS + "/**")
                                .hasAuthority("admin.group.write")
                        // Menus
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.MENUS, AdminApiPaths.MENUS + "/**")
                                .hasAuthority("admin.menu.read")
                        .requestMatchers(AdminApiPaths.MENUS, AdminApiPaths.MENUS + "/**")
                                .hasAuthority("admin.menu.write")
                        // Tenants
                        .requestMatchers(HttpMethod.GET, base + "/tenants", base + "/tenants/**")
                                .hasAuthority("admin.tenant.read")
                        .requestMatchers(base + "/tenants", base + "/tenants/**")
                                .hasAuthority("admin.tenant.write")
                        // Audit logs (read-only)
                        .requestMatchers(AdminApiPaths.AUDIT_LOGS, AdminApiPaths.AUDIT_LOGS + "/**")
                                .hasAuthority("admin.audit.read")
                        // Policies (dry-run)
                        .requestMatchers(base + "/policies", base + "/policies/**")
                                .hasAuthority("admin.policy.test")
                        // Diagnostics (probes)
                        .requestMatchers(base + "/diagnostics", base + "/diagnostics/**")
                                .hasAuthority("admin.diagnostics.run")
                        // Settings (read-only)
                        .requestMatchers(base + "/settings", base + "/settings/**")
                                .hasAuthority("admin.settings.read")
                        // Self-service (e.g. change-password) and anything else under the base path.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
