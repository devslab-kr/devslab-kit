package kr.devslab.kit.tenant;

import java.util.Optional;

public interface TenantContextHolder {

    Optional<TenantContext> current();

    void set(TenantContext context);

    void clear();
}
