package kr.devslab.kit.identity;

import java.util.Objects;
import kr.devslab.kit.core.id.TenantId;

public record LoginCommand(TenantId tenantId, String loginId, String rawPassword) {

    public LoginCommand {
        Objects.requireNonNull(tenantId, "LoginCommand tenantId must not be null");
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("LoginCommand loginId must not be null or blank");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("LoginCommand rawPassword must not be null or empty");
        }
    }
}
