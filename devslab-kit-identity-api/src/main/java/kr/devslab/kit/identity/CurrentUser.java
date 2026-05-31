package kr.devslab.kit.identity;

import java.util.Set;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record CurrentUser(
        UserId id,
        PublicId publicId,
        TenantId tenantId,
        String loginId,
        UserStatus status,
        Set<String> roles,
        boolean mustChangePassword
) {

    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Convenience overload for callers that don't carry the forced-rotation flag. */
    public CurrentUser(
            UserId id,
            PublicId publicId,
            TenantId tenantId,
            String loginId,
            UserStatus status,
            Set<String> roles
    ) {
        this(id, publicId, tenantId, loginId, status, roles, false);
    }
}
