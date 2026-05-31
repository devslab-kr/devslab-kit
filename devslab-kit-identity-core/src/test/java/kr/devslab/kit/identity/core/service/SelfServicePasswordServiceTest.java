package kr.devslab.kit.identity.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.UserStatus;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SelfServicePasswordServiceTest {

    private final PasswordHasher passwordHasher = new BCryptPasswordHasher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JpaPlatformUserAccountRepository repository;

    private SelfServicePasswordService service;
    private UUID userId;
    private PlatformUserAccountEntity account;

    @BeforeEach
    void setUp() {
        service = new SelfServicePasswordService(repository, passwordHasher, clock);
        userId = UUID.randomUUID();
        account = new PlatformUserAccountEntity(
                userId,
                "usr_" + userId,
                "default",
                "admin",
                "admin@example.com",
                passwordHasher.hash("oldpass"),
                UserStatus.ACTIVE,
                false,
                "LOCAL",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
        account.setMustChangePassword(true);
        lenient().when(repository.findById(userId)).thenReturn(Optional.of(account));
    }

    @Test
    void changesPasswordAndClearsForcedRotationFlag() {
        service.changePassword(UserId.of(userId), "oldpass", "brand-new-password");

        assertThat(passwordHasher.matches("brand-new-password", account.getPasswordHash())).isTrue();
        assertThat(passwordHasher.matches("oldpass", account.getPasswordHash())).isFalse();
        assertThat(account.isMustChangePassword()).isFalse();
        assertThat(account.getUpdatedAt()).isEqualTo(Instant.parse("2026-05-31T00:00:00Z"));
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutMutating() {
        String originalHash = account.getPasswordHash();

        assertThatThrownBy(() -> service.changePassword(UserId.of(userId), "WRONG", "brand-new-password"))
                .isInstanceOf(AccountLoginException.class)
                .extracting(ex -> ((AccountLoginException) ex).reason())
                .isEqualTo(LoginFailureReason.BAD_CREDENTIALS);

        assertThat(account.getPasswordHash()).isEqualTo(originalHash);
        assertThat(account.isMustChangePassword()).isTrue();
    }

    @Test
    void throwsUnknownUserWhenAccountMissing() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(UserId.of(missing), "x", "brand-new-password"))
                .isInstanceOf(AccountLoginException.class)
                .extracting(ex -> ((AccountLoginException) ex).reason())
                .isEqualTo(LoginFailureReason.UNKNOWN_USER);
    }
}
