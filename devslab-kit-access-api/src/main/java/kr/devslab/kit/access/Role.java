package kr.devslab.kit.access;

import java.util.Objects;
import kr.devslab.kit.core.id.RoleId;

public record Role(RoleId id, String code, String name) {

    public Role {
        Objects.requireNonNull(id, "Role id must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Role code must not be null or blank");
        }
    }
}
