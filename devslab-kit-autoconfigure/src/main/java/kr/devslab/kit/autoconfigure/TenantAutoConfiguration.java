package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.tenant.TenantContextHolder;
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.core.DefaultTenantContextHolder;
import kr.devslab.kit.tenant.core.FixedTenantResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(DevslabKitProperties.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.tenant",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantContextHolder tenantContextHolder() {
        return new DefaultTenantContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantResolver tenantResolver(DevslabKitProperties properties) {
        return new FixedTenantResolver(properties.getTenant().getDefaultTenantId());
    }
}
