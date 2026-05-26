package kr.devslab.kit.admin.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.audit.core.service.AuditLogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.AUDIT_LOGS)
public class AuditLogAdminController {

    private final AuditLogQueryService service;

    public AuditLogAdminController(AuditLogQueryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditLogResponse> queryByTenant(
            @RequestParam String tenantId,
            @RequestParam(required = false) Instant since
    ) {
        Instant cutoff = since == null ? Instant.EPOCH : since;
        return service.findByTenantSince(tenantId, cutoff).stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/user/{userId}")
    public List<AuditLogResponse> queryByUser(@PathVariable UUID userId) {
        return service.findByUser(userId).stream().map(AuditLogResponse::from).toList();
    }
}
