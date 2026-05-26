package kr.devslab.kit.access;

public record Permission(String code) {

    public Permission {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Permission code must not be null or blank");
        }
    }

    public static Permission of(String code) {
        return new Permission(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
