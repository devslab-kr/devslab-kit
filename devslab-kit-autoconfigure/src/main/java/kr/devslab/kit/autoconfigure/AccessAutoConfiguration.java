package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.List;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.core.repository.JpaPlatformGroupRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformGroupRoleRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRolePermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformUserGroupRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformUserRoleRepository;
import kr.devslab.kit.access.core.service.DefaultPermissionChecker;
import kr.devslab.kit.access.core.service.DefaultPolicyEvaluator;
import kr.devslab.kit.access.core.service.GroupMembershipService;
import kr.devslab.kit.access.core.service.GroupRoleService;
import kr.devslab.kit.access.core.service.GroupService;
import kr.devslab.kit.access.core.service.PermissionGrantQueryService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyEvaluator;
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
    public GroupService groupService(JpaPlatformGroupRepository repository, Clock clock) {
        return new GroupService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupMembershipService groupMembershipService(JpaPlatformUserGroupRepository repository, Clock clock) {
        return new GroupMembershipService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupRoleService groupRoleService(JpaPlatformGroupRoleRepository repository, Clock clock) {
        return new GroupRoleService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyEvaluator policyEvaluator(List<Policy> policies) {
        return new DefaultPolicyEvaluator(policies);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(CurrentUserProvider.class)
    public PermissionChecker permissionChecker(
            CurrentUserProvider currentUserProvider,
            JpaPlatformPermissionRepository permissionRepository,
            PolicyEvaluator policyEvaluator
    ) {
        return new DefaultPermissionChecker(currentUserProvider, permissionRepository, policyEvaluator);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionGrantQueryService permissionGrantQueryService(
            JpaPlatformPermissionRepository permissionRepository
    ) {
        return new PermissionGrantQueryService(permissionRepository);
    }
}
