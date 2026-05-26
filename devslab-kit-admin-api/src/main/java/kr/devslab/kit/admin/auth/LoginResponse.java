package kr.devslab.kit.admin.auth;

import java.time.Instant;
import java.util.Set;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.CurrentUser;

public record LoginResponse(String token, Instant expiresAt, UserSummary user) {

    public static LoginResponse of(AuthToken token, CurrentUser user) {
        return new LoginResponse(
                token.value(),
                token.expiresAt(),
                new UserSummary(
                        user.id().value().toString(),
                        user.publicId().value(),
                        user.tenantId().value(),
                        user.loginId(),
                        user.status().name(),
                        user.roles()
                )
        );
    }

    public record UserSummary(
            String id,
            String publicId,
            String tenantId,
            String loginId,
            String status,
            Set<String> roles
    ) {
    }
}
