package kr.devslab.kit.identity.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AccountLoginException;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.LoginCommand;
import kr.devslab.kit.identity.LoginFailureReason;
import kr.devslab.kit.identity.LoginResult;
import kr.devslab.kit.identity.PasswordHasher;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.event.LoginFailedEvent;
import kr.devslab.kit.identity.event.LoginSucceededEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class LocalLoginService {

    private final JpaPlatformUserAccountRepository repository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public LocalLoginService(
            JpaPlatformUserAccountRepository repository,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Instant now = Instant.now(clock);
        TenantId tenantId = command.tenantId();
        String loginId = command.loginId();

        Optional<PlatformUserAccountEntity> maybeAccount =
                repository.findByTenantIdAndLoginId(tenantId.value(), loginId);

        if (maybeAccount.isEmpty()) {
            failAndThrow(tenantId, loginId, LoginFailureReason.UNKNOWN_USER, now);
        }
        PlatformUserAccountEntity account = maybeAccount.get();

        if (account.isLocked()) {
            failAndThrow(tenantId, loginId, LoginFailureReason.ACCOUNT_LOCKED, now);
        }
        switch (account.getStatus()) {
            case DISABLED -> failAndThrow(tenantId, loginId, LoginFailureReason.ACCOUNT_DISABLED, now);
            case PENDING_VERIFICATION ->
                    failAndThrow(tenantId, loginId, LoginFailureReason.PENDING_VERIFICATION, now);
            case LOCKED -> failAndThrow(tenantId, loginId, LoginFailureReason.ACCOUNT_LOCKED, now);
            case ACTIVE -> { /* proceed */ }
        }

        if (!passwordHasher.matches(command.rawPassword(), account.getPasswordHash())) {
            failAndThrow(tenantId, loginId, LoginFailureReason.BAD_CREDENTIALS, now);
        }

        UserId userId = UserId.of(account.getId());
        eventPublisher.publishEvent(new LoginSucceededEvent(userId, tenantId, loginId, now));

        CurrentUser currentUser = new CurrentUser(
                userId,
                PublicId.of(account.getPublicId()),
                tenantId,
                loginId,
                account.getStatus(),
                Set.of()
        );
        return new LoginResult(currentUser);
    }

    private void failAndThrow(TenantId tenantId, String loginId, LoginFailureReason reason, Instant now) {
        eventPublisher.publishEvent(new LoginFailedEvent(tenantId, loginId, reason, now));
        throw new AccountLoginException(reason);
    }
}
