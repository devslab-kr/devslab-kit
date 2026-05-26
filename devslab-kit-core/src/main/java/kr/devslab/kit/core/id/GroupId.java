package kr.devslab.kit.core.id;

import java.util.Objects;
import java.util.UUID;

public record GroupId(UUID value) {

    public GroupId {
        Objects.requireNonNull(value, "GroupId value must not be null");
    }

    public static GroupId of(UUID value) {
        return new GroupId(value);
    }

    public static GroupId of(String value) {
        return new GroupId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
