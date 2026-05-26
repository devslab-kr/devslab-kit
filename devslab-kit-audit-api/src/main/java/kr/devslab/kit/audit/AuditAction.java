package kr.devslab.kit.audit;

public record AuditAction(String code) {

    public AuditAction {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("AuditAction code must not be null or blank");
        }
    }

    public static AuditAction of(String code) {
        return new AuditAction(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
