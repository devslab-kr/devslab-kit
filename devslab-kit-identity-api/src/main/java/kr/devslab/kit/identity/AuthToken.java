package kr.devslab.kit.identity;

import java.time.Instant;
import java.util.Objects;

public record AuthToken(String value, Instant issuedAt, Instant expiresAt) {

    public AuthToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AuthToken value must not be null or blank");
        }
        Objects.requireNonNull(issuedAt, "AuthToken issuedAt must not be null");
        Objects.requireNonNull(expiresAt, "AuthToken expiresAt must not be null");
    }
}
