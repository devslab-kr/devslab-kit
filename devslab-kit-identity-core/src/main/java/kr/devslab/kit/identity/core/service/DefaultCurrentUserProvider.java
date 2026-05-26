package kr.devslab.kit.identity.core.service;

import java.util.Optional;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class DefaultCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CurrentUser currentUser) {
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }
}
