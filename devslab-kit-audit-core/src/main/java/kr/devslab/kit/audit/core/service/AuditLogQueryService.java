package kr.devslab.kit.audit.core.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import org.springframework.transaction.annotation.Transactional;

public class AuditLogQueryService {

    private final JpaPlatformAuditLogRepository repository;

    public AuditLogQueryService(JpaPlatformAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PlatformAuditLogEntity> findByTenantSince(String tenantId, Instant since) {
        return repository.findAllByActorTenantIdAndOccurredAtAfter(tenantId, since);
    }

    @Transactional(readOnly = true)
    public List<PlatformAuditLogEntity> findByUser(UUID userId) {
        return repository.findAllByActorUserId(userId);
    }
}
