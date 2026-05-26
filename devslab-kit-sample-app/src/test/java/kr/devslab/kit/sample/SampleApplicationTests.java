package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void contextLoads() {
        assertThat(tenantResolver).isNotNull();
        assertThat(tenantContextHolder).isNotNull();
    }

    @Test
    void fixedTenantResolverReturnsConfiguredTenant() {
        assertThat(tenantResolver.resolve().tenantId().value()).isEqualTo("default");
    }
}
