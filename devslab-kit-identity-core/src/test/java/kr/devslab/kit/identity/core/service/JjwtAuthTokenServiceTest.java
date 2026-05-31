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

    /**
     * Regression lock for the parser-clock bug: {@code parse()} must validate
     * expiry against the <em>injected</em> clock, not the real system clock.
     *
     * <p>The clock is fixed far in the past, so the token's 8h window closed
     * years ago in wall-clock terms. If {@code parse()} (re)introduces the
     * default system clock, it sees the token as long expired and returns
     * empty — and this assertion fails. It passes only while the injected clock
     * governs expiry. Deterministic regardless of when CI runs.
     */
    @Test
    void parseHonorsInjectedClock_acceptsTokenThatRealClockWouldReject() {
        Clock past = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC);
        JjwtAuthTokenService pastService =
                new JjwtAuthTokenService(SECRET, Duration.ofHours(8), "devslab-kit-test", past);

        AuthToken token = pastService.issue(user(false));

        assertThat(pastService.parse(token.value())).isPresent();
    }

    /** And the symmetric case: a token past its TTL per the injected clock is rejected. */
    @Test
    void parseRejectsTokenExpiredPerInjectedClock() {
        Clock t0 = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);
        JjwtAuthTokenService issuerService =
                new JjwtAuthTokenService(SECRET, Duration.ofHours(1), "devslab-kit-test", t0);
        AuthToken token = issuerService.issue(user(false));

        // A reader whose clock sits 2h later — past the 1h TTL — must reject it.
        Clock later = Clock.fixed(Instant.parse("2026-05-31T02:00:00Z"), ZoneOffset.UTC);
        JjwtAuthTokenService readerService =
                new JjwtAuthTokenService(SECRET, Duration.ofHours(1), "devslab-kit-test", later);

        assertThat(readerService.parse(token.value())).isEmpty();
    }
}
