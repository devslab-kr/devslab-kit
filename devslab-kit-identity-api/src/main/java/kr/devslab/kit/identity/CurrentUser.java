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
        Set<String> roles
) {

    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
