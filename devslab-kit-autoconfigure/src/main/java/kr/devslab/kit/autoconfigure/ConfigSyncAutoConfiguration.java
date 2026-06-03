package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.admin.config.ConfigExportService;
import kr.devslab.kit.admin.config.ConfigImportService;
import kr.devslab.kit.admin.config.ConfigSyncController;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Config sync (ADR 0003) — **OFF by default**. The whole surface activates only when
 * {@code devslab.kit.config-sync.enabled=true}. This is the environment-promotion tool
 * (export/import a code-keyed config bundle); it is a dev/staging convenience, never a
 * production default.
 *
 * <p>Prototype scope (PR 1): the read-only {@code GET /config/export}. {@code POST /import}
 * (merge + dry-run) and the stronger gating (only under a dev/local profile, fail-fast if
 * enabled in production) follow per the ADR's PR breakdown.
 */
@AutoConfiguration(after = AdminApiAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "devslab.kit.config-sync", name = "enabled", havingValue = "true")
@Import(ConfigSyncController.class)
public class ConfigSyncAutoConfiguration {

    @Bean
    ConfigExportService configExportService(
            PermissionAdminService permissions,
            RoleAdminService roles,
            RolePermissionService rolePermissions,
            MenuAdminService menus,
            PlatformUserAccountAdminService userAccounts,
            UserRoleService userRoles
    ) {
        return new ConfigExportService(permissions, roles, rolePermissions, menus, userAccounts, userRoles);
    }

    @Bean
    ConfigImportService configImportService(
            PermissionAdminService permissions,
            RoleAdminService roles,
            RolePermissionService rolePermissions,
            MenuAdminService menus,
            PlatformUserAccountAdminService userAccounts,
            UserRoleService userRoles
    ) {
        return new ConfigImportService(permissions, roles, rolePermissions, menus, userAccounts, userRoles);
    }

    /** Refuses to start if config sync is enabled under a production profile (ADR 0003 §5). */
    @Bean
    ConfigSyncProductionGuard configSyncProductionGuard(Environment environment) {
        return new ConfigSyncProductionGuard(environment);
    }
}

