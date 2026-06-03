package kr.devslab.kit.admin.config;

import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config sync endpoints (ADR 0003). Registered and gated by
 * {@code ConfigSyncAutoConfiguration} — the whole surface is off unless
 * {@code devslab.kit.config-sync.enabled=true}.
 *
 * <p>Prototype scope: {@code export} + {@code import} ({@code merge} mode, dry-run by
 * default). {@code mirror} mode, optional user sync, and the dev-profile / prod fail-fast
 * gating follow per the ADR's PR breakdown.
 */
@RestController
@RequestMapping(AdminApiPaths.BASE + "/config")
public class ConfigSyncController {

    private final ConfigExportService exportService;
    private final ConfigImportService importService;

    public ConfigSyncController(ConfigExportService exportService, ConfigImportService importService) {
        this.exportService = exportService;
        this.importService = importService;
    }

    /** Export a tenant's definitional config as a portable, code-keyed bundle. */
    @GetMapping("/export")
    public ConfigBundle export(@RequestParam String tenantId) {
        return exportService.export(TenantId.of(tenantId));
    }

    /**
     * Apply a bundle by natural code. {@code dryRun=true} (the default) returns the diff
     * without writing. {@code mode=merge} (the default) is additive and never deletes.
     */
    @PostMapping("/import")
    public ImportResult importConfig(
            @RequestBody ConfigBundle bundle,
            @RequestParam(defaultValue = "merge") String mode,
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        return importService.apply(bundle, mode, dryRun);
    }
}
