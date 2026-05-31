package kr.devslab.kit.identity.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.UserStatus;
import org.junit.jupiter.api.Test;

class JjwtAuthTokenServiceTest {

    private static final String SECRET = "test-only-32byte-jwt-signing-key!!";
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);
    private final JjwtAuthTokenService service =
            new JjwtAuthTokenService(SECRET, Duration.ofHours(8), "devslab-kit-test", clock);

    private CurrentUser user(boolean mustChangePassword) {
        UUID id = UUID.randomUUID();
        return new CurrentUser(
                UserId.of(id),
                PublicId.of("usr_" + id),
                TenantId.of("default"),
                "admin",
                UserStatus.ACTIVE,
                Set.of("PLATFORM_ADMIN"),
                mustChangePassword
        );
    }

    @Test
    void roundTripsMustChangePasswordTrue() {
        AuthToken token = service.issue(user(true));

        CurrentUser parsed = service.parse(token.value()).orElseThrow();

        assertThat(parsed.mustChangePassword()).isTrue();
        assertThat(parsed.roles()).containsExactly("PLATFORM_ADMIN");
        assertThat(parsed.loginId()).isEqualTo("admin");
    }

    @Test
    void roundTripsMustChangePasswordFalse() {
        AuthToken token = service.issue(user(false));

        CurrentUser parsed = service.parse(token.value()).orElseThrow();

        assertThat(parsed.mustChangePassword()).isFalse();
    }
}
