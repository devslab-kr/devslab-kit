package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves {@code devslab.kit.bootstrap.seed} provisions starter RBAC on boot: the
 * declared permissions, roles and grants are created, a permission referenced only
 * by a role is auto-created, and grants are precise (a role gets exactly what it
 * lists — no over-granting). Seeding is idempotent by construction (each step is a
 * find-by-code-then-create / idempotent grant), so a re-boot against this same data
 * is a no-op.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "devslab.kit.bootstrap.enabled=true",
            "devslab.kit.bootstrap.seed.permissions[0]=tasks.read",
            "devslab.kit.bootstrap.seed.permissions[1]=tasks.write",
            "devslab.kit.bootstrap.seed.permissions[2]=tasks.update",
            "devslab.kit.bootstrap.seed.permissions[3]=tasks.delete",
            "devslab.kit.bootstrap.seed.roles.viewer[0]=tasks.read",
            "devslab.kit.bootstrap.seed.roles.editor[0]=tasks.read",
            "devslab.kit.bootstrap.seed.roles.editor[1]=tasks.write",
            "devslab.kit.bootstrap.seed.roles.editor[2]=tasks.update",
            "devslab.kit.bootstrap.seed.roles.owner[0]=tasks.read",
            "devslab.kit.bootstrap.seed.roles.owner[1]=tasks.write",
            "devslab.kit.bootstrap.seed.roles.owner[2]=tasks.update",
            "devslab.kit.bootstrap.seed.roles.owner[3]=tasks.delete",
            // referenced only by a role, absent from the permissions list — must be auto-created:
            "devslab.kit.bootstrap.seed.roles.owner[4]=tasks.export"
        })
class BootstrapSeedTests {

    private static final String TENANT = "default";

    @Autowired
    private JpaPlatformPermissionRepository permissions;

    @Autowired
    private JpaPlatformRoleRepository roles;

    @Autowired
    private RolePermissionService rolePermissions;

    @Test
    void seedsDeclaredPermissions() {
        for (String code : List.of("tasks.read", "tasks.write", "tasks.update", "tasks.delete")) {
            assertThat(permissions.findByCode(code)).as("permission %s", code).isPresent();
        }
    }

    @Test
    void autoCreatesAPermissionReferencedOnlyByARole() {
        assertThat(permissions.findByCode("tasks.export")).isPresent();
    }

    @Test
    void seedsRolesInTheBootstrapTenant() {
        assertThat(roles.findByTenantIdAndCode(TENANT, "viewer")).isPresent();
        assertThat(roles.findByTenantIdAndCode(TENANT, "editor")).isPresent();
        assertThat(roles.findByTenantIdAndCode(TENANT, "owner")).isPresent();
    }

    @Test
    void grantsAreExactPerRole() {
        var editor = roles.findByTenantIdAndCode(TENANT, "editor").orElseThrow();
        List<PermissionId> editorPerms = rolePermissions.findPermissionIdsForRole(RoleId.of(editor.getId()));

        PermissionId read = permissionId("tasks.read");
        PermissionId write = permissionId("tasks.write");
        PermissionId update = permissionId("tasks.update");
        PermissionId delete = permissionId("tasks.delete");

        assertThat(editorPerms).contains(read, write, update);
        assertThat(editorPerms).doesNotContain(delete); // editor never listed delete
    }

    private PermissionId permissionId(String code) {
        return PermissionId.of(permissions.findByCode(code).orElseThrow().getId());
    }
}
