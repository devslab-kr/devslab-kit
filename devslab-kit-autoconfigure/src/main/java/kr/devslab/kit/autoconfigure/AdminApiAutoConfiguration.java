package kr.devslab.kit.autoconfigure;

import java.time.Clock;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import kr.devslab.kit.audit.core.service.AuditLogQueryService;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {IdentityAutoConfiguration.class, AccessAutoConfiguration.class, MenuAutoConfiguration.class, AuditAutoConfiguration.class})
@ConditionalOnClass(jakarta.persistence.EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.admin-api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AdminApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PlatformUserAccountAdminService platformUserAccountAdminService(
            JpaPlatformUserAccountRepository repository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        return new PlatformUserAccountAdminService(repository, passwordHasher, eventPublisher, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoleAdminService roleAdminService(JpaPlatformRoleRepository repository, Clock clock) {
        return new RoleAdminService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionAdminService permissionAdminService(JpaPlatformPermissionRepository repository, Clock clock) {
        return new PermissionAdminService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public MenuAdminService menuAdminService(JpaPlatformMenuRepository repository, Clock clock) {
        return new MenuAdminService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogQueryService auditLogQueryService(JpaPlatformAuditLogRepository repository) {
        return new AuditLogQueryService(repository);
    }
}
