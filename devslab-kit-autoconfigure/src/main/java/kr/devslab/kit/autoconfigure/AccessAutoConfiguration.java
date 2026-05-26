package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRolePermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformUserRoleRepository;
import kr.devslab.kit.access.core.service.DefaultPermissionChecker;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.identity.CurrentUserProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
}, after = IdentityAutoConfiguration.class)
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.access",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AccessAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UserRoleService userRoleService(JpaPlatformUserRoleRepository repository, Clock clock) {
        return new UserRoleService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public RolePermissionService rolePermissionService(JpaPlatformRolePermissionRepository repository, Clock clock) {
        return new RolePermissionService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CurrentUserProvider.class)
    public PermissionChecker permissionChecker(
            CurrentUserProvider currentUserProvider,
            JpaPlatformPermissionRepository permissionRepository
    ) {
        return new DefaultPermissionChecker(currentUserProvider, permissionRepository);
    }
}
