package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
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
import org.springframework.cache.CacheManager;
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
     * The database-backed {@link DefaultMenuProvider}, wrapped in a
     * {@link CachingMenuProvider} riding the shared {@link CacheManager} when one
     * is present (ADR 0002 §5). The cache backend — in-memory, Redis, or no-op —
     * follows {@code devslab.kit.cache.type}; TTL and distribution are the cache
     * manager's concern, not this provider's. When no {@code CacheManager} bean
     * exists (caching disabled entirely), the bare database-backed provider is
     * returned, so menus always work.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({PermissionBasedMenuFilter.class})
    public MenuProvider menuProvider(
            JpaPlatformMenuRepository repository,
            MenuTreeBuilder menuTreeBuilder,
            PermissionBasedMenuFilter filter,
            org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManager
    ) {
        MenuProvider base = new DefaultMenuProvider(repository, menuTreeBuilder, filter);
        CacheManager cm = cacheManager.getIfAvailable();
        if (cm == null) {
            return base;
        }
        return new CachingMenuProvider(base, cm);
    }
}
