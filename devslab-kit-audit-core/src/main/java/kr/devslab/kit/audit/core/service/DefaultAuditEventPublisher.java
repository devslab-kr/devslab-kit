package kr.devslab.kit.audit.core.service;

import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;
import org.springframework.context.ApplicationEventPublisher;

public class DefaultAuditEventPublisher implements AuditEventPublisher {

    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultAuditEventPublisher(
            AuditLogService auditLogService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.auditLogService = auditLogService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(AuditEvent event) {
        auditLogService.record(event);
        applicationEventPublisher.publishEvent(event);
    }
}
