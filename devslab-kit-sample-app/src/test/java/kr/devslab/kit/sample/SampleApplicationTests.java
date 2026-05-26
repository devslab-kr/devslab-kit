package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.identity.CurrentUserProvider;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.core.service.LocalLoginService;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.tenant.TenantContextHolder;
import kr.devslab.kit.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SampleApplicationTests {

    @Autowired
    private TenantResolver tenantResolver;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private LocalLoginService localLoginService;

    @Autowired
    private PermissionChecker permissionChecker;

    @Autowired
    private MenuProvider menuProvider;

    @Autowired
    private AuditEventPublisher auditEventPublisher;

    @Test
    void contextLoads() {
        assertThat(tenantResolver).isNotNull();
        assertThat(tenantContextHolder).isNotNull();
        assertThat(currentUserProvider).isNotNull();
        assertThat(passwordHasher).isNotNull();
        assertThat(localLoginService).isNotNull();
        assertThat(permissionChecker).isNotNull();
        assertThat(menuProvider).isNotNull();
        assertThat(auditEventPublisher).isNotNull();
    }

    @Test
    void fixedTenantResolverReturnsConfiguredTenant() {
        assertThat(tenantResolver.resolve().tenantId().value()).isEqualTo("default");
    }

    @Test
    void bcryptPasswordHasherRoundTrips() {
        String hash = passwordHasher.hash("hunter2");
        assertThat(passwordHasher.matches("hunter2", hash)).isTrue();
        assertThat(passwordHasher.matches("wrong", hash)).isFalse();
    }
}
