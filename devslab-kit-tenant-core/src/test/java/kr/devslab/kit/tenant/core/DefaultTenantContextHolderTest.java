package kr.devslab.kit.tenant.core;

import static org.assertj.core.api.Assertions.assertThat;

import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DefaultTenantContextHolderTest {

    private final DefaultTenantContextHolder holder = new DefaultTenantContextHolder();

    @AfterEach
    void clearHolder() {
        holder.clear();
    }

    @Test
    void currentReturnsEmptyWhenUnset() {
        assertThat(holder.current()).isEmpty();
    }

    @Test
    void setAndCurrentAndClearRoundTrip() {
        var ctx = new TenantContext(TenantId.of("acme"));

        holder.set(ctx);

        assertThat(holder.current()).contains(ctx);

        holder.clear();

        assertThat(holder.current()).isEmpty();
    }
}
