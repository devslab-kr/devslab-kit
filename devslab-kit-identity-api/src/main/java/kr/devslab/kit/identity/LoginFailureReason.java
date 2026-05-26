package kr.devslab.kit.identity;

public enum LoginFailureReason {
    UNKNOWN_USER,
    BAD_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,
    PENDING_VERIFICATION
}
