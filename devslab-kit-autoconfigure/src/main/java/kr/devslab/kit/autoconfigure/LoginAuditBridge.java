package kr.devslab.kit.autoconfigure;

import java.util.Map;
import kr.devslab.kit.audit.AuditAction;
import kr.devslab.kit.audit.AuditActor;
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.audit.AuditOutcome;
import kr.devslab.kit.audit.AuditTarget;
import kr.devslab.kit.identity.event.LoginFailedEvent;
import kr.devslab.kit.identity.event.LoginSucceededEvent;
import org.springframework.context.event.EventListener;

/**
 * Bridges identity login events onto the audit log.
 *
 * <p>{@code identity-core} publishes {@link LoginSucceededEvent} /
 * {@link LoginFailedEvent} as plain Spring application events (it cannot
 * depend on {@code audit-api} without coupling the two verticals). This
 * bridge — which lives in {@code autoconfigure}, the one module that sees
 * both — listens for them and re-publishes an {@link AuditEvent} through
 * {@link AuditEventPublisher}, so every login attempt lands in
 * {@code platform_audit_log} and shows up on the admin UI's Audit Logs
 * page.
 *
 * <p>Both events map to the single action code {@code identity.login};
 * success vs. failure is carried by {@link AuditOutcome} (the column the
 * Audit Logs page filters on), keeping the action filter clean.
 *
 * <p>IP / user-agent are left null here — the identity events don't carry
 * the HTTP request context. A servlet-aware enrichment can populate them
 * later without changing this bridge's shape.
 */
public class LoginAuditBridge {

    static final String ACTION_LOGIN = "identity.login";
    static final String TARGET_TYPE_USER = "USER";

    private final AuditEventPublisher auditEventPublisher;

    public LoginAuditBridge(AuditEventPublisher auditEventPublisher) {
        this.auditEventPublisher = auditEventPublisher;
    }

    @EventListener
    public void onLoginSucceeded(LoginSucceededEvent event) {
        AuditActor actor = new AuditActor(event.userId(), event.tenantId(), event.loginId());
        AuditTarget target = new AuditTarget(TARGET_TYPE_USER, event.userId().value().toString());
        auditEventPublisher.publish(AuditEvent.builder()
                .action(AuditAction.of(ACTION_LOGIN))
                .actor(actor)
                .target(target)
                .occurredAt(event.occurredAt())
                .outcome(AuditOutcome.SUCCESS)
                .build());
    }

    @EventListener
    public void onLoginFailed(LoginFailedEvent event) {
        // userId is unknown on failure (bad login id, wrong password, locked
        // account...), so the actor carries only the tenant + the attempted
        // login id, and the failure reason rides along in the metadata.
        AuditActor actor = new AuditActor(null, event.tenantId(), event.loginId());
        AuditTarget target = new AuditTarget(TARGET_TYPE_USER, event.loginId());
        auditEventPublisher.publish(AuditEvent.builder()
                .action(AuditAction.of(ACTION_LOGIN))
                .actor(actor)
                .target(target)
                .occurredAt(event.occurredAt())
                .outcome(AuditOutcome.FAILURE)
                .metadata(Map.of("reason", event.reason().name()))
                .build());
    }
}
