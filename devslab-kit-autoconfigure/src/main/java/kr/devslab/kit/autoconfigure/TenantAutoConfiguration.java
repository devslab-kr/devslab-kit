package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.tenant.TenantContextHolder;
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.core.DefaultTenantContextHolder;
import kr.devslab.kit.tenant.core.FixedTenantResolver;
import kr.devslab.kit.tenant.core.HeaderTenantResolver;
import kr.devslab.kit.tenant.core.SubdomainTenantResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

@AutoConfiguration
@EnableConfigurationProperties(DevslabKitProperties.class)
@ImportRuntimeHints(DevslabKitRuntimeHints.class)
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
        DevslabKitProperties.Tenant tenant = properties.getTenant();
        return switch (tenant.getResolver()) {
            case FIXED -> new FixedTenantResolver(tenant.getDefaultTenantId());
            case HEADER -> new HeaderTenantResolver(tenant.getHeaderName(), tenant.getDefaultTenantId());
            case SUBDOMAIN -> new SubdomainTenantResolver(tenant.getSubdomainIndex(), tenant.getDefaultTenantId());
            case JWT -> throw new IllegalStateException(
                    "JWT TenantResolver requires the devslab-kit-oauth2-resource-server-starter (not yet shipped). "
                            + "Provide a custom TenantResolver bean for now."
            );
            case CUSTOM -> throw new IllegalStateException(
                    "Resolver mode CUSTOM requires the consumer app to define its own TenantResolver bean. "
                            + "Add one and the @ConditionalOnMissingBean default will step aside."
            );
        };
    }
}
