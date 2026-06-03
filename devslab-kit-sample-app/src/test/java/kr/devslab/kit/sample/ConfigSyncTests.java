package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.admin.config.ConfigBundle;
import kr.devslab.kit.admin.config.ConfigBundle.MenuDef;
import kr.devslab.kit.admin.config.ConfigBundle.PermissionDef;
import kr.devslab.kit.admin.config.ConfigBundle.RoleDef;
import kr.devslab.kit.admin.config.ConfigExportService;
import kr.devslab.kit.admin.config.ConfigImportService;
import kr.devslab.kit.admin.config.ImportResult;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * ADR 0003 config-sync prototype — export + import round-trip over real Postgres.
 * Also proves the feature is gated: these beans only exist because
 * {@code devslab.kit.config-sync.enabled=true} here.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "devslab.kit.config-sync.enabled=true")
class ConfigSyncTests {

    @Autowired
    private ConfigExportService exportService;

    @Autowired
    private ConfigImportService importService;

    @Autowired
    private PermissionAdminService permissions;

    @Autowired
    private RoleAdminService roles;

    @Autowired
    private RolePermissionService rolePermissions;

    @Autowired
    private MenuAdminService menus;

    private final TenantId tenant = TenantId.of("default");

    @Test
    void exportsDefinitionalConfigKeyedByCode() {
        permissions.create("configsync.read", "config-sync test permission");
        UUID permId = permissionId("configsync.read");
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

    @Test
    void importCreatesByCodeRespectsParentOrderAndIsIdempotent() {
        // Child is listed BEFORE its parent to exercise parent-before-child ordering.
        ConfigBundle bundle = new ConfigBundle(
                ConfigBundle.CURRENT_VERSION,
                "default",
                List.of(new PermissionDef("cs.import.read", "imported permission")),
                List.of(new RoleDef("CS_IMPORTER", "CS Importer", List.of("cs.import.read"))),
                List.of(
                        new MenuDef("cs-child", "cs-parent", "Child", "/cs/child", null, "cs.import.read", 2),
                        new MenuDef("cs-parent", null, "Parent", "/cs-imp", null, null, 1)));

        // dry-run: reports what would be created, writes nothing.
        ImportResult dry = importService.apply(bundle, "merge", true);
        assertThat(dry.dryRun()).isTrue();
        assertThat(dry.permissions().created()).contains("cs.import.read");
        assertThat(dry.roles().created()).contains("CS_IMPORTER");
        assertThat(dry.menus().created()).contains("cs-parent", "cs-child");
        assertThat(permissions.listAll()).noneMatch(p -> p.getCode().equals("cs.import.read"));

        // apply: creates everything, by code, parent before child.
        ImportResult applied = importService.apply(bundle, "merge", false);
        assertThat(applied.dryRun()).isFalse();
        assertThat(applied.menus().created()).contains("cs-parent", "cs-child");

        assertThat(permissions.listAll()).anyMatch(p -> p.getCode().equals("cs.import.read"));
        var importer = roles.listByTenant(tenant).stream()
                .filter(r -> r.code().equals("CS_IMPORTER")).findFirst().orElseThrow();
        assertThat(rolePermissions.findPermissionIdsForRole(importer.id()))
                .extracting(PermissionId::value)
                .contains(permissionId("cs.import.read"));
        var menuList = menus.listByTenant(tenant);
        var parent = menuList.stream().filter(m -> m.getCode().equals("cs-parent")).findFirst().orElseThrow();
        var child = menuList.stream().filter(m -> m.getCode().equals("cs-child")).findFirst().orElseThrow();
        assertThat(child.getParentId()).isEqualTo(parent.getId());
        assertThat(child.getRequiredPermissionCode()).isEqualTo("cs.import.read");

        // idempotent: re-applying the same bundle creates nothing.
        ImportResult again = importService.apply(bundle, "merge", false);
        assertThat(again.permissions().created()).isEmpty();
        assertThat(again.roles().created()).isEmpty();
        assertThat(again.menus().created()).isEmpty();
    }

    private UUID permissionId(String code) {
        return permissions.listAll().stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst().orElseThrow().getId();
    }
}
