package kr.devslab.kit.tenant.core;

import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.tenant.TenantResolver;

public class FixedTenantResolver implements TenantResolver {

    private final TenantContext fixedContext;

    public FixedTenantResolver(String tenantId) {
        this.fixedContext = new TenantContext(TenantId.of(tenantId));
    }

    @Override
    public TenantContext resolve() {
        return fixedContext;
    }
}
