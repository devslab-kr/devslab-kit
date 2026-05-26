package kr.devslab.kit.tenant.core;

import java.util.Optional;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.tenant.TenantContextHolder;

public class DefaultTenantContextHolder implements TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    @Override
    public Optional<TenantContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    @Override
    public void set(TenantContext context) {
        HOLDER.set(context);
    }

    @Override
    public void clear() {
        HOLDER.remove();
    }
}
