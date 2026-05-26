package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
import kr.devslab.kit.menu.core.service.DefaultMenuProvider;
import kr.devslab.kit.menu.core.service.MenuTreeBuilder;
import kr.devslab.kit.menu.core.service.PermissionBasedMenuFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
}, after = AccessAutoConfiguration.class)
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.menu",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MenuAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MenuTreeBuilder menuTreeBuilder() {
        return new MenuTreeBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(PermissionChecker.class)
    public PermissionBasedMenuFilter permissionBasedMenuFilter(PermissionChecker permissionChecker) {
        return new PermissionBasedMenuFilter(permissionChecker);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({PermissionBasedMenuFilter.class})
    public MenuProvider menuProvider(
            JpaPlatformMenuRepository repository,
            MenuTreeBuilder menuTreeBuilder,
            PermissionBasedMenuFilter filter
    ) {
        return new DefaultMenuProvider(repository, menuTreeBuilder, filter);
    }
}
