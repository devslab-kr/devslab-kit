package kr.devslab.kit.identity;

import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record UserAccountView(
        UserId id,
        PublicId publicId,
        TenantId tenantId,
        String loginId,
        String email,
        UserStatus status,
        boolean locked,
        String providerType
) {
}
