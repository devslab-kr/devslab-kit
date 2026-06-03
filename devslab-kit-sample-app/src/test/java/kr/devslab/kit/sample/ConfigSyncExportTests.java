package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.admin.config.ConfigBundle;
import kr.devslab.kit.admin.config.ConfigExportService;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * ADR 0003 config-sync prototype — proves the export side: a tenant's definitional config
 * (permissions, roles + their permission codes, menus) is captured into a portable bundle
 * keyed by natural codes. Also proves the feature is gated: {@code ConfigExportService} is
 * only a bean because {@code devslab.kit.config-sync.enabled=true} here.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "devslab.kit.config-sync.enabled=true")
class ConfigSyncExportTests {

    @Autowired
    private ConfigExportService exportService;

    @Autowired
    private PermissionAdminService permissions;

    @Autowired
    private RoleAdminService roles;

    @Autowired
    private RolePermissionService rolePermissions;

    @Autowired
    private MenuAdminService menus;

    @Test
    void exportsDefinitionalConfigKeyedByCode() {
        TenantId tenant = TenantId.of("default");

        permissions.create("configsync.read", "config-sync test permission");
        UUID permId = permissions.listAll().stream()
                .filter(p -> p.getCode().equals("configsync.read"))
                .findFirst().orElseThrow().getId();
        var role = roles.create(tenant, "CONFIGSYNC_TESTER", "Config Sync Tester");
        rolePermissions.grant(role.id(), PermissionId.of(permId));
        menus.create(tenant, "configsync-menu", "Config Sync", "/cs", null, 5, "configsync.read", "pi-sync");

        ConfigBundle bundle = exportService.export(tenant);

        assertThat(bundle.version()).isEqualTo(ConfigBundle.CURRENT_VERSION);
        assertThat(bundle.tenantId()).isEqualTo("default");
        assertThat(bundle.permissions()).anyMatch(p -> p.code().equals("configsync.read"));
        assertThat(bundle.roles())
                .filteredOn(r -> r.code().equals("CONFIGSYNC_TESTER"))
                .singleElement()
                .satisfies(r -> assertThat(r.permissionCodes()).contains("configsync.read"));
        assertThat(bundle.menus())
                .filteredOn(m -> m.code().equals("configsync-menu"))
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.requiredPermissionCode()).isEqualTo("configsync.read");
                    assertThat(m.label()).isEqualTo("Config Sync");
                    assertThat(m.path()).isEqualTo("/cs");
                });
    }
}
