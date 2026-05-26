package kr.devslab.kit.audit;

public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
