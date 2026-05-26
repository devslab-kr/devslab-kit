package kr.devslab.kit.core.id;

import java.util.Objects;
import java.util.UUID;

public record RoleId(UUID value) {

    public RoleId {
        Objects.requireNonNull(value, "RoleId value must not be null");
    }

    public static RoleId of(UUID value) {
        return new RoleId(value);
    }

    public static RoleId of(String value) {
        return new RoleId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
