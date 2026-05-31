package kr.devslab.kit.admin.bootstrap;

/**
 * Whether the platform has been provisioned with at least one account.
 *
 * <p>{@code initialized == false} means a fresh, un-provisioned instance — no
 * one can log in yet. A guided first-run / setup wizard branches on this to
 * decide whether to show a "create the first administrator" form (ADR 0001).
 */
public record BootstrapStatusResponse(boolean initialized) {
}
