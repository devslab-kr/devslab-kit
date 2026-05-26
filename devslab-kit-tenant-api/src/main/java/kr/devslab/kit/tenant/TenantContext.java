package kr.devslab.kit.tenant;

import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;

public record TenantContext(TenantId tenantId) {

    public TenantContext {
        Objects.requireNonNull(tenantId, "TenantContext requires a non-null TenantId");
    }

    public static TenantContext of(TenantId tenantId) {
        return new TenantContext(tenantId);
    }
}
