package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.UserStatus;
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.core.JwtTenantResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Proves {@code resolver: jwt} actually works: the kit wires {@link JwtTenantResolver},
 * it derives the active tenant from the kit-issued bearer token's tenant claim, and it
 * falls back to the configured default tenant when there is no token.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "devslab.kit.tenant.mode=multi",
            "devslab.kit.tenant.resolver=jwt",
            "devslab.kit.tenant.default-tenant-id=default"
        })
class JwtTenantResolverTests {

    @Autowired
    private TenantResolver resolver;

    @Autowired
    private AuthTokenService tokens;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void wiresTheJwtResolver() {
        assertThat(resolver).isInstanceOf(JwtTenantResolver.class);
    }

    @Test
    void resolvesTenantFromBearerToken() {
        CurrentUser user = new CurrentUser(
                UserId.of(UUID.randomUUID()),
                PublicId.of("pub-acme"),
                TenantId.of("acme"),
                "alice",
                UserStatus.ACTIVE,
                Set.of(),
                false);
        String token = tokens.issue(user).value();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThat(resolver.resolve().tenantId().value()).isEqualTo("acme");
    }

    @Test
    void fallsBackToDefaultWithoutAToken() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThat(resolver.resolve().tenantId().value()).isEqualTo("default");
    }
}
