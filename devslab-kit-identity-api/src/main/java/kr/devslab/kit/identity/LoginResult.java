package kr.devslab.kit.identity;

import java.util.Objects;

public record LoginResult(CurrentUser user) {

    public LoginResult {
        Objects.requireNonNull(user, "LoginResult user must not be null");
    }
}
