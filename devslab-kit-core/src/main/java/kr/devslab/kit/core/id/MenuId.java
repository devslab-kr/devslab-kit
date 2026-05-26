package kr.devslab.kit.core.id;

import java.util.Objects;
import java.util.UUID;

public record MenuId(UUID value) {

    public MenuId {
        Objects.requireNonNull(value, "MenuId value must not be null");
    }

    public static MenuId of(UUID value) {
        return new MenuId(value);
    }

    public static MenuId of(String value) {
        return new MenuId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
