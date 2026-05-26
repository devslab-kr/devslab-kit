package kr.devslab.kit.core.id;

public record PublicId(String value) {

    public PublicId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PublicId value must not be null or blank");
        }
    }

    public static PublicId of(String value) {
        return new PublicId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
