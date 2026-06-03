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
 * <p>{@code export} + {@code import} support {@code merge} / {@code mirror} modes (dry-run by
 * default) and optional user sync ({@code includeUsers}). The whole surface is refused under
 * a production profile (ADR 0003 §5).
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

    /**
     * Export a tenant's config as a portable, code-keyed bundle. Definitional config
     * (permissions, roles, menus) is always included; users only when
     * {@code includeUsers=true} (and even then with no password).
     */
    @GetMapping("/export")
    public ConfigBundle export(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "false") boolean includeUsers
    ) {
        return exportService.export(TenantId.of(tenantId), includeUsers);
    }

    /**
     * Apply a bundle by natural code. {@code dryRun=true} (the default) returns the diff
     * without writing. {@code mode=merge} (the default) is additive and never deletes;
     * {@code mode=mirror} also deletes entities absent from the bundle. {@code includeUsers=true}
     * additionally creates missing users from the bundle (existing users are never overwritten).
     */
    @PostMapping("/import")
    public ImportResult importConfig(
            @RequestBody ConfigBundle bundle,
            @RequestParam(defaultValue = "merge") String mode,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean includeUsers
    ) {
        return importService.apply(bundle, mode, dryRun, includeUsers);
    }
}
