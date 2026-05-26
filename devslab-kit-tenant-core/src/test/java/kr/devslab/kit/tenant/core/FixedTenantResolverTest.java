package kr.devslab.kit.tenant.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FixedTenantResolverTest {

    @Test
    void resolveAlwaysReturnsConfiguredTenant() {
        var resolver = new FixedTenantResolver("acme");

        assertThat(resolver.resolve().tenantId().value()).isEqualTo("acme");
        assertThat(resolver.resolve().tenantId().value()).isEqualTo("acme");
    }
}
