package kr.devslab.kit.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.audit.core.service.AuditLogQueryService;
import kr.devslab.kit.audit.core.service.AuditLogQueryService.AuditLogSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.AUDIT_LOGS)
public class AuditLogAdminController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogQueryService service;
    private final ObjectMapper objectMapper;

    public AuditLogAdminController(AuditLogQueryService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /**
     * Filterable, paginated search backing the admin UI's Audit Logs page.
     * All filter params are optional; defaults are 25 rows per page sorted
     * by {@code occurredAt DESC}. {@code page} is 0-based.
     */
    @GetMapping
    public Page<AuditLogResponse> search(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String actorLogin,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "25") int size
    ) {
        AuditLogSearchCriteria criteria = new AuditLogSearchCriteria(
                tenantId, actorLogin, action, targetType, outcome, from, to);
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.clamp(size == 0 ? DEFAULT_PAGE_SIZE : size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
        return service.search(criteria, pageable).map(entity -> AuditLogResponse.from(entity, objectMapper));
    }

    /**
     * Convenience read used by the diagnostics page — flat list, no paging.
     */
    @GetMapping("/user/{userId}")
    public List<AuditLogResponse> queryByUser(@PathVariable UUID userId) {
        return service.findByUser(userId).stream()
                .map(e -> AuditLogResponse.from(e, objectMapper))
                .toList();
    }
}
