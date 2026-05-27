package kr.devslab.kit.audit.core.service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    /**
     * Filterable, paginated search backing {@code GET /admin/api/v1/audit-logs}.
     *
     * <p>All filter fields are optional — null means "any". The returned page is
     * Spring Data's standard {@link Page}, sorted by {@code occurredAt} descending
     * unless the caller overrides via {@link Pageable#getSort()}.
     */
    @Transactional(readOnly = true)
    public Page<PlatformAuditLogEntity> search(AuditLogSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(toSpecification(criteria), pageable);
    }

    private Specification<PlatformAuditLogEntity> toSpecification(AuditLogSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.tenantId() != null && !c.tenantId().isBlank()) {
                predicates.add(cb.equal(root.get("actorTenantId"), c.tenantId()));
            }
            if (c.actorLogin() != null && !c.actorLogin().isBlank()) {
                // Case-insensitive contains, matches the entity field that stores
                // the actor's display name / login at event time.
                predicates.add(cb.like(cb.lower(root.get("actorDisplayName")),
                        "%" + c.actorLogin().toLowerCase() + "%"));
            }
            if (c.action() != null && !c.action().isBlank()) {
                predicates.add(cb.equal(root.get("actionCode"), c.action()));
            }
            if (c.targetType() != null && !c.targetType().isBlank()) {
                predicates.add(cb.equal(root.get("targetType"), c.targetType()));
            }
            if (c.outcome() != null && !c.outcome().isBlank()) {
                predicates.add(cb.equal(root.get("outcome"), c.outcome()));
            }
            if (c.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), c.from()));
            }
            if (c.to() != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), c.to()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public record AuditLogSearchCriteria(
            String tenantId,
            String actorLogin,
            String action,
            String targetType,
            String outcome,
            Instant from,
            Instant to
    ) {
    }
}
