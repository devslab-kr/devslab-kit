package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.admin.config.ConfigBundle;
import kr.devslab.kit.admin.config.ConfigBundle.MenuDef;
import kr.devslab.kit.admin.config.ConfigBundle.PermissionDef;
import kr.devslab.kit.admin.config.ConfigBundle.RoleDef;
import kr.devslab.kit.admin.config.ConfigBundle.UserDef;
import kr.devslab.kit.admin.config.ConfigExportService;
import kr.devslab.kit.admin.config.ConfigImportService;
import kr.devslab.kit.admin.config.ImportResult;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
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

    @Autowired
    private PlatformUserAccountAdminService userAccounts;

    @Autowired
    private UserRoleService userRoles;

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

    @Test
    void mirrorDeletesExtrasReconcilesGrantsAndSkipsRolesAssignedToUsers() {
        // Seed: a permission to keep + one to drop, a role granted both, two menus.
        permissions.create("mir.keep", "keep me");
        permissions.create("mir.drop", "drop me");
        UUID dropPermId = permissionId("mir.drop");
        var role = roles.create(tenant, "MIR_ROLE", "Mirror Role");
        rolePermissions.grant(role.id(), PermissionId.of(permissionId("mir.keep")));
        rolePermissions.grant(role.id(), PermissionId.of(dropPermId));
        menus.create(tenant, "mir-keep", "Keep", "/mk", null, 1, null, null);
        menus.create(tenant, "mir-drop", "Drop", "/md", null, 2, null, null);
        // A role assigned to a user must survive mirror (skipped, never stripped).
        var inUse = roles.create(tenant, "MIR_INUSE", "In Use");
        var user = userAccounts.create(tenant, "mir-user", "mir@example.com", "pw-12345678", "LOCAL");
        userRoles.assign(user.id(), inUse.id(), tenant);

        // Build the mirror bundle from the current full export, minus what should go: the
        // mir.drop permission (and its grant on MIR_ROLE), the mir-drop menu, the MIR_INUSE
        // role. Everything else in the DB stays in the bundle, so mirror leaves it alone.
        ConfigBundle full = exportService.export(tenant);
        var perms = full.permissions().stream()
                .filter(p -> !p.code().equals("mir.drop")).toList();
        var rolesMinus = full.roles().stream()
                .filter(r -> !r.code().equals("MIR_INUSE"))
                .map(r -> r.code().equals("MIR_ROLE")
                        ? new RoleDef(r.code(), r.name(),
                                r.permissionCodes().stream().filter(c -> !c.equals("mir.drop")).toList())
                        : r)
                .toList();
        var menusMinus = full.menus().stream()
                .filter(m -> !m.code().equals("mir-drop")).toList();
        ConfigBundle bundle = new ConfigBundle(full.version(), full.tenantId(), perms, rolesMinus, menusMinus);

        // dry-run reports the deletes/skips without writing anything.
        ImportResult dry = importService.apply(bundle, "mirror", true);
        assertThat(dry.mode()).isEqualTo("mirror");
        assertThat(dry.permissions().deleted()).contains("mir.drop");
        assertThat(dry.menus().deleted()).contains("mir-drop");
        assertThat(dry.roles().skipped()).contains("MIR_INUSE");
        assertThat(dry.roles().deleted()).doesNotContain("MIR_INUSE");
        assertThat(permissions.listAll()).anyMatch(p -> p.getCode().equals("mir.drop"));

        // apply
        ImportResult applied = importService.apply(bundle, "mirror", false);
        assertThat(applied.permissions().deleted()).contains("mir.drop");
        assertThat(applied.menus().deleted()).contains("mir-drop");
        assertThat(applied.roles().updated()).contains("MIR_ROLE"); // grant reconciled
        assertThat(applied.roles().skipped()).contains("MIR_INUSE");

        // mir.drop permission gone, mir.keep stays.
        assertThat(permissions.listAll()).noneMatch(p -> p.getCode().equals("mir.drop"));
        assertThat(permissions.listAll()).anyMatch(p -> p.getCode().equals("mir.keep"));
        // MIR_ROLE no longer grants the dropped permission.
        assertThat(rolePermissions.findPermissionIdsForRole(role.id()))
                .extracting(PermissionId::value)
                .doesNotContain(dropPermId);
        // mir-drop menu gone, mir-keep stays.
        assertThat(menus.listByTenant(tenant)).noneMatch(m -> m.getCode().equals("mir-drop"));
        assertThat(menus.listByTenant(tenant)).anyMatch(m -> m.getCode().equals("mir-keep"));
        // MIR_INUSE survived (still assigned to a user).
        assertThat(roles.listByTenant(tenant)).anyMatch(r -> r.code().equals("MIR_INUSE"));
    }

    @Test
    void userSyncExportsWithoutSecretsAndCreatesMissingUsersOnly() {
        permissions.create("us.read", "user-sync read");
        var role = roles.create(tenant, "US_ROLE", "User Sync Role");
        rolePermissions.grant(role.id(), PermissionId.of(permissionId("us.read")));
        var existing = userAccounts.create(tenant, "us-existing", "ex@example.com", "pw-12345678", "LOCAL");
        userRoles.assign(existing.id(), role.id(), tenant);

        // Definitional export omits users; includeUsers carries them — by code, no password.
        assertThat(exportService.export(tenant).users()).isEmpty();
        ConfigBundle withUsers = exportService.export(tenant, true);
        assertThat(withUsers.users())
                .filteredOn(u -> u.loginId().equals("us-existing"))
                .singleElement()
                .satisfies(u -> {
                    assertThat(u.email()).isEqualTo("ex@example.com");
                    assertThat(u.status()).isEqualTo("ACTIVE");
                    assertThat(u.roleCodes()).contains("US_ROLE");
                });

        // Import a brand-new user (create-only). includeUsers=true is required.
        ConfigBundle bundle = new ConfigBundle(
                ConfigBundle.CURRENT_VERSION,
                "default",
                List.of(),
                List.of(new RoleDef("US_ROLE", "User Sync Role", List.of("us.read"))),
                List.of(),
                List.of(new UserDef("us-new", "new@example.com", "ACTIVE", List.of("US_ROLE"))));

        // dry-run: reports, writes nothing.
        ImportResult dry = importService.apply(bundle, "merge", true, true);
        assertThat(dry.users().created()).contains("us-new");
        assertThat(userAccounts.listByTenant(tenant)).noneMatch(u -> u.loginId().equals("us-new"));

        // apply: creates the user and assigns the role by code.
        ImportResult applied = importService.apply(bundle, "merge", false, true);
        assertThat(applied.users().created()).contains("us-new");
        var created = userAccounts.listByTenant(tenant).stream()
                .filter(u -> u.loginId().equals("us-new")).findFirst().orElseThrow();
        assertThat(userRoles.findRoleIdsForUser(created.id()))
                .extracting(RoleId::value)
                .contains(role.id().value());

        // idempotent + never overwrite: re-apply reports skipped, not created.
        ImportResult again = importService.apply(bundle, "merge", false, true);
        assertThat(again.users().created()).isEmpty();
        assertThat(again.users().skipped()).contains("us-new");

        // includeUsers=false ignores bundle users entirely.
        ConfigBundle ignoreUsers = new ConfigBundle(
                ConfigBundle.CURRENT_VERSION, "default",
                List.of(), List.of(), List.of(),
                List.of(new UserDef("us-ignored", "ig@example.com", "ACTIVE", List.of())));
        ImportResult noUsers = importService.apply(ignoreUsers, "merge", false, false);
        assertThat(noUsers.users().created()).isEmpty();
        assertThat(userAccounts.listByTenant(tenant)).noneMatch(u -> u.loginId().equals("us-ignored"));
    }

    private UUID permissionId(String code) {
        return permissions.listAll().stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst().orElseThrow().getId();
    }
}
