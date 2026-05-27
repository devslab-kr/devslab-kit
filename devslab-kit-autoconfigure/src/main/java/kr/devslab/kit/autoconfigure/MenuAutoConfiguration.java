package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
import kr.devslab.kit.menu.core.service.CachingMenuProvider;
import kr.devslab.kit.menu.core.service.DefaultMenuProvider;
import kr.devslab.kit.menu.core.service.MenuTreeBuilder;
import kr.devslab.kit.menu.core.service.PermissionBasedMenuFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(DevslabKitProperties.class)
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

    /**
     * Wraps the database-backed {@link DefaultMenuProvider} in a
     * {@link CachingMenuProvider} when {@code devslab.kit.menu.cache-ttl} is
     * positive (default 5m). A zero / negative TTL skips the cache decorator
     * entirely, which is the right behaviour for tests that mutate menus or
     * permissions and expect the next read to see the change immediately.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({PermissionBasedMenuFilter.class})
    public MenuProvider menuProvider(
            JpaPlatformMenuRepository repository,
            MenuTreeBuilder menuTreeBuilder,
            PermissionBasedMenuFilter filter,
            DevslabKitProperties properties,
            Clock clock
    ) {
        MenuProvider base = new DefaultMenuProvider(repository, menuTreeBuilder, filter);
        Duration ttl = properties.getMenu().getCacheTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return base;
        }
        return new CachingMenuProvider(base, ttl, clock);
    }
}
