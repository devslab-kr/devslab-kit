package kr.devslab.kit.core.id;

import java.util.Objects;
import java.util.UUID;

public record PermissionId(UUID value) {

    public PermissionId {
        Objects.requireNonNull(value, "PermissionId value must not be null");
    }

    public static PermissionId of(UUID value) {
        return new PermissionId(value);
    }

    public static PermissionId of(String value) {
        return new PermissionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
