package kr.devslab.kit.admin.config;

import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config sync endpoints (ADR 0003). Registered and gated by
 * {@code ConfigSyncAutoConfiguration} — the whole surface is off unless
 * {@code devslab.kit.config-sync.enabled=true}.
 *
 * <p>Export only for now (prototype PR 1); {@code POST /import} (merge + dry-run) and the
 * dev-profile / prod fail-fast gating follow per the ADR's PR breakdown.
 */
@RestController
@RequestMapping(AdminApiPaths.BASE + "/config")
public class ConfigSyncController {

    private final ConfigExportService exportService;

    public ConfigSyncController(ConfigExportService exportService) {
        this.exportService = exportService;
    }

    /** Export a tenant's definitional config as a portable, code-keyed bundle. */
    @GetMapping("/export")
    public ConfigBundle export(@RequestParam String tenantId) {
        return exportService.export(TenantId.of(tenantId));
    }
}
