package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.tenant.TenantService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Wires the first-admin bootstrap runner (ADR 0001) when
 * {@code devslab.kit.bootstrap.enabled=true}. OFF by default — there is no
 * {@code matchIfMissing}, so a deploy with no bootstrap config provisions
 * nothing and cannot grow an accidental backdoor.
 *
 * <p>Ordered after the identity / access / tenant / admin-api auto-configs so
 * all the services it needs are already defined.
 */
@AutoConfiguration(after = {
        IdentityAutoConfiguration.class,
        AccessAutoConfiguration.class,
        TenantAutoConfiguration.class,
        AdminApiAutoConfiguration.class
})
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(prefix = "devslab.kit.bootstrap", name = "enabled", havingValue = "true")
public class BootstrapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DevslabKitBootstrapRunner devslabKitBootstrapRunner(
            DevslabKitProperties properties,
            Environment environment,
            TenantService tenantService,
            PermissionAdminService permissionAdminService,
            JpaPlatformPermissionRepository permissionRepository,
            RoleAdminService roleAdminService,
            JpaPlatformRoleRepository roleRepository,
            RolePermissionService rolePermissionService,
            PlatformUserAccountAdminService userAdminService,
            JpaPlatformUserAccountRepository userRepository,
            UserRoleService userRoleService
    ) {
        return new DevslabKitBootstrapRunner(
                properties.getBootstrap(),
                environment,
                tenantService,
                permissionAdminService,
                permissionRepository,
                roleAdminService,
                roleRepository,
                rolePermissionService,
                userAdminService,
                userRepository,
                userRoleService
        );
    }
}
