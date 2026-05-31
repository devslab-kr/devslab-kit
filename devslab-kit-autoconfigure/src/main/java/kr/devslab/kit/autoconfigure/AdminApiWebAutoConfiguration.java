package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import kr.devslab.kit.admin.AdminApiExceptionHandler;
import kr.devslab.kit.admin.audit.AuditLogAdminController;
import kr.devslab.kit.admin.auth.AuthController;
import kr.devslab.kit.admin.bootstrap.BootstrapStatusController;
import kr.devslab.kit.admin.diagnostics.DiagnosticsController;
import kr.devslab.kit.admin.group.GroupAdminController;
import kr.devslab.kit.admin.menu.MenuAdminController;
import kr.devslab.kit.admin.permission.PermissionAdminController;
import kr.devslab.kit.admin.policy.PolicyAdminController;
import kr.devslab.kit.admin.role.RoleAdminController;
import kr.devslab.kit.admin.security.AdminSecurityConfig;
import kr.devslab.kit.admin.settings.SettingsAdminController;
import kr.devslab.kit.admin.tenant.TenantAdminController;
import kr.devslab.kit.admin.user.UserAdminController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-registers devslab-kit's admin REST API <em>web</em> layer — the
 * {@code @RestController}s under {@code kr.devslab.kit.admin.*}, the
 * {@code @RestControllerAdvice} error handler, and the {@link AdminSecurityConfig}
 * filter chain — so the documented promise holds: a consumer with a plain
 * {@code @SpringBootApplication} in any package gets {@code /admin/api/v1/**} working
 * with no {@code scanBasePackages}.
 *
 * <p>These are the only kit beans that are <em>not</em> contributed by an
 * {@code @Bean} method elsewhere (the services, repositories and entities all are);
 * the controllers were previously discovered only by the consumer component-scanning
 * {@code kr.devslab.kit}. They are {@code @Import}ed explicitly here — rather than via
 * an {@code @ComponentScan} inside an auto-configuration (which Spring Boot advises
 * against) — so registration is deterministic and the set is auditable. Each controller's
 * collaborators are ordinary beans from {@link AdminApiAutoConfiguration} and the other
 * kit auto-configs; {@code AuthController}'s {@code AuthenticationManager} /
 * {@code UserDetailsService} come from Boot's own Spring Security auto-configuration.
 *
 * <p>Guards: only in a servlet web app, only when Spring Security ({@link SecurityFilterChain})
 * and Spring MVC ({@link DispatcherServlet}) are present, and only while
 * {@code devslab.kit.admin-api.enabled} is not {@code false} (matching
 * {@link AdminApiAutoConfiguration}, which contributes the services these controllers need and
 * therefore must run first). {@link AdminSecurityConfig}'s beans are
 * {@code @ConditionalOnMissingBean}, so a consumer can still supply their own security.
 */
@AutoConfiguration(after = AdminApiAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EntityManager.class, SecurityFilterChain.class, DispatcherServlet.class})
@ConditionalOnProperty(
        prefix = "devslab.kit.admin-api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import({
        AdminSecurityConfig.class,
        AdminApiExceptionHandler.class,
        AuthController.class,
        BootstrapStatusController.class,
        UserAdminController.class,
        RoleAdminController.class,
        PermissionAdminController.class,
        GroupAdminController.class,
        MenuAdminController.class,
        TenantAdminController.class,
        PolicyAdminController.class,
        AuditLogAdminController.class,
        SettingsAdminController.class,
        DiagnosticsController.class
})
public class AdminApiWebAutoConfiguration {
}
