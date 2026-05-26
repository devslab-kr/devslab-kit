package kr.devslab.kit.audit;

public record AuditTarget(String type, String id) {

    public AuditTarget {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("AuditTarget type must not be null or blank");
        }
    }
}
