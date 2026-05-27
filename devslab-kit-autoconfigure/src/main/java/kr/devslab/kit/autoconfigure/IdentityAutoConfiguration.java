package kr.devslab.kit.autoconfigure;

import java.time.Clock;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUserProvider;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.BCryptPasswordHasher;
import kr.devslab.kit.identity.core.service.DefaultCurrentUserProvider;
import kr.devslab.kit.identity.core.service.JjwtAuthTokenService;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import kr.devslab.kit.identity.core.service.PlatformUserAccountService;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.identity",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalLoginService localLoginService(
            JpaPlatformUserAccountRepository repository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            DevslabKitProperties properties
    ) {
        DevslabKitProperties.Identity identity = properties.getIdentity();
        return new LocalLoginService(
                repository,
                passwordHasher,
                eventPublisher,
                clock,
                identity.getMaxFailedAttempts(),
                identity.getLockoutDuration()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformUserAccountService platformUserAccountService(JpaPlatformUserAccountRepository repository) {
        return new PlatformUserAccountService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentUserProvider currentUserProvider() {
        return new DefaultCurrentUserProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthTokenService authTokenService(Clock clock, DevslabKitProperties properties) {
        DevslabKitProperties.Identity.Jwt jwt = properties.getIdentity().getJwt();
        return new JjwtAuthTokenService(jwt.getSecret(), jwt.getTtl(), jwt.getIssuer(), clock);
    }
}
