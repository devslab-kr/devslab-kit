package kr.devslab.kit.identity;

import java.util.Optional;

public interface AuthTokenService {

    AuthToken issue(CurrentUser user);

    Optional<CurrentUser> parse(String token);
}
