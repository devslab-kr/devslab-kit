package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.tenant.TenantContextHolder;
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.TenantService;
import kr.devslab.kit.tenant.core.DefaultTenantContextHolder;
import kr.devslab.kit.tenant.core.FixedTenantResolver;
import kr.devslab.kit.tenant.core.HeaderTenantResolver;
import kr.devslab.kit.tenant.core.JwtTenantResolver;
import kr.devslab.kit.tenant.core.SubdomainTenantResolver;
import kr.devslab.kit.tenant.core.repository.JpaPlatformTenantRepository;
import kr.devslab.kit.tenant.core.service.DefaultTenantService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    public TenantResolver tenantResolver(
            DevslabKitProperties properties,
            ObjectProvider<AuthTokenService> authTokenService
    ) {
        DevslabKitProperties.Tenant tenant = properties.getTenant();
        return switch (tenant.getResolver()) {
            case FIXED -> new FixedTenantResolver(tenant.getDefaultTenantId());
            case HEADER -> new HeaderTenantResolver(tenant.getHeaderName(), tenant.getDefaultTenantId());
            case SUBDOMAIN -> new SubdomainTenantResolver(tenant.getSubdomainIndex(), tenant.getDefaultTenantId());
            case JWT -> {
                AuthTokenService tokens = authTokenService.getIfAvailable();
                if (tokens == null) {
                    throw new IllegalStateException(
                            "resolver: jwt requires an AuthTokenService bean (the identity module); it reads "
                                    + "the tenant claim from the kit-issued bearer token."
                    );
                }
                yield new JwtTenantResolver(tokens, tenant.getDefaultTenantId());
            }
            case CUSTOM -> throw new IllegalStateException(
                    "Resolver mode CUSTOM requires the consumer app to define its own TenantResolver bean. "
                            + "Add one and the @ConditionalOnMissingBean default will step aside."
            );
        };
    }

    @Bean
    @ConditionalOnClass(EntityManager.class)
    @ConditionalOnBean(JpaPlatformTenantRepository.class)
    @ConditionalOnMissingBean
    public TenantService tenantService(JpaPlatformTenantRepository repository, Clock clock) {
        return new DefaultTenantService(repository, clock);
    }
}
