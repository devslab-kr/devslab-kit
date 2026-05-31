package kr.devslab.kit.identity.core.service;

import java.time.Clock;
import java.time.Instant;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service password change for the currently authenticated account.
 *
 * <p>Distinct from {@link PlatformUserAccountAdminService#resetPassword}
 * (an administrator resetting <em>someone else's</em> password with no
 * knowledge of the old one). Here the caller proves ownership by supplying
 * the current password, and a successful change also clears the
 * {@code must_change_password} flag — this is the endpoint that releases a
 * bootstrap admin from the forced-rotation gate (ADR 0001).
 */
public class SelfServicePasswordService {

    private final JpaPlatformUserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public SelfServicePasswordService(
            JpaPlatformUserAccountRepository repository,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    /**
     * Verify {@code oldRawPassword} against the stored hash, then store
     * {@code newRawPassword} and clear the forced-rotation flag.
     *
     * @throws AccountLoginException with {@link LoginFailureReason#UNKNOWN_USER}
     *         if the id no longer resolves (e.g. account deleted mid-session),
     *         or {@link LoginFailureReason#BAD_CREDENTIALS} if the supplied
     *         current password does not match.
     */
    @Transactional
    public void changePassword(UserId id, String oldRawPassword, String newRawPassword) {
        PlatformUserAccountEntity account = repository.findById(id.value())
                .orElseThrow(() -> new AccountLoginException(LoginFailureReason.UNKNOWN_USER));

        if (account.getPasswordHash() == null
                || !passwordHasher.matches(oldRawPassword, account.getPasswordHash())) {
            throw new AccountLoginException(LoginFailureReason.BAD_CREDENTIALS);
        }

        account.setPasswordHash(passwordHasher.hash(newRawPassword));
        account.setMustChangePassword(false);
        account.setUpdatedAt(Instant.now(clock));
    }
}
