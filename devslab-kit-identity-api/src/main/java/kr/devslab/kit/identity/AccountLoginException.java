package kr.devslab.kit.identity;

import kr.devslab.kit.core.exception.DevslabKitException;

public class AccountLoginException extends DevslabKitException {

    private final LoginFailureReason reason;

    public AccountLoginException(LoginFailureReason reason) {
        super("Login failed: " + reason);
        this.reason = reason;
    }

    public LoginFailureReason reason() {
        return reason;
    }
}
