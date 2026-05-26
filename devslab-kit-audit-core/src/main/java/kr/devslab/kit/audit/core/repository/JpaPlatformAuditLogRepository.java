package kr.devslab.kit.audit.core.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPlatformAuditLogRepository extends JpaRepository<PlatformAuditLogEntity, UUID> {

    List<PlatformAuditLogEntity> findAllByActorTenantIdAndOccurredAtAfter(String actorTenantId, Instant occurredAt);

    List<PlatformAuditLogEntity> findAllByActorUserId(UUID actorUserId);
}
