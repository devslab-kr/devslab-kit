package kr.devslab.kit.identity;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<CurrentUser> current();
}
