package kr.devslab.kit.audit;

/**
 * Two-state outcome for an {@link AuditEvent}.
 *
 * <p>Stored verbatim in the {@code platform_audit_log.outcome} column and
 * surfaced under the same name in the admin UI's Audit Logs page.
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
