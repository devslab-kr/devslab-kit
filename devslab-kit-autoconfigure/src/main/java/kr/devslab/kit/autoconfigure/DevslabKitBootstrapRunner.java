package kr.devslab.kit.autoconfigure;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.tenant.TenantMetadata;
import kr.devslab.kit.tenant.TenantMode;
import kr.devslab.kit.tenant.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent first-admin bootstrap (ADR 0001).
 *
 * <p>When {@code devslab.kit.bootstrap.enabled=true}, provisions just enough on
 * first boot for the dashboard to be usable end-to-end:
 *
 * <ul>
 *   <li>The configured tenant (default {@code default}).</li>
 *   <li>An admin role (default {@code PLATFORM_ADMIN}) bound to that tenant.</li>
 *   <li>The {@code admin.*} permission set covering every admin-api endpoint,
 *       granted to that role.</li>
 *   <li>One admin user (default {@code admin}) holding the role.</li>
 * </ul>
 *
 * <p>Every step is idempotent — re-boots find each entity by code / loginId and
 * skip creation, so running against an already-provisioned database is a no-op.
 *
 * <p>Password handling follows the GitLab / Jenkins pattern: a blank
 * {@code admin-password} means "generate a strong random one and log it exactly
 * once". A fixed password only exists when an operator wrote it into config —
 * which is what a local-dev profile does and what a production profile must not.
 * The prod safety pin fails startup if a well-known weak password is configured
 * under a {@code prod}/{@code production} profile.
 */
public class DevslabKitBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevslabKitBootstrapRunner.class);

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "admin", "password", "passwd", "changeme", "change-me", "admin123",
            "root", "123456", "12345678", "secret", "default", "test"
    );

    private static final List<PermissionSeed> ADMIN_PERMISSIONS = List.of(
            new PermissionSeed("admin.user.read", "Read user accounts via /admin/api/v1/users"),
            new PermissionSeed("admin.user.write", "Create / update / delete user accounts"),
            new PermissionSeed("admin.role.read", "Read roles and their permission grants"),
            new PermissionSeed("admin.role.write", "Create / rename / delete roles and grant permissions"),
            new PermissionSeed("admin.permission.read", "Read permission catalogue"),
            new PermissionSeed("admin.permission.write", "Create / update / delete permissions"),
            new PermissionSeed("admin.group.read", "Read groups and memberships"),
            new PermissionSeed("admin.group.write", "Create / rename / delete groups and manage memberships"),
            new PermissionSeed("admin.menu.read", "Read menu tree"),
            new PermissionSeed("admin.menu.write", "Create / update / delete menu items"),
            new PermissionSeed("admin.tenant.read", "Read tenants"),
            new PermissionSeed("admin.tenant.write", "Create / rename / change status / delete tenants"),
            new PermissionSeed("admin.policy.test", "Run policy dry-run via /admin/api/v1/policies/test"),
            new PermissionSeed("admin.audit.read", "Read audit logs with filters + paging"),
            new PermissionSeed("admin.diagnostics.run", "Run diagnostics probes (login-test, permission-check, menu-visibility)"),
            new PermissionSeed("admin.settings.read", "Read DevslabKitProperties via /admin/api/v1/settings")
    );

    private final DevslabKitProperties.Bootstrap props;
    private final Environment environment;
    private final TenantService tenantService;
    private final PermissionAdminService permissionAdminService;
    private final JpaPlatformPermissionRepository permissionRepository;
    private final RoleAdminService roleAdminService;
    private final JpaPlatformRoleRepository roleRepository;
    private final RolePermissionService rolePermissionService;
    private final PlatformUserAccountAdminService userAdminService;
    private final JpaPlatformUserAccountRepository userRepository;
    private final UserRoleService userRoleService;

    public DevslabKitBootstrapRunner(
            DevslabKitProperties.Bootstrap props,
            Environment environment,
            TenantService tenantService,
            PermissionAdminService permissionAdminService,
            JpaPlatformPermissionRepository permissionRepository,
            RoleAdminService roleAdminService,
            JpaPlatformRoleRepository roleRepository,
            RolePermissionService rolePermissionService,
            PlatformUserAccountAdminService userAdminService,
            JpaPlatformUserAccountRepository userRepository,
            UserRoleService userRoleService
    ) {
        this.props = props;
        this.environment = environment;
        this.tenantService = tenantService;
        this.permissionAdminService = permissionAdminService;
        this.permissionRepository = permissionRepository;
        this.roleAdminService = roleAdminService;
        this.roleRepository = roleRepository;
        this.rolePermissionService = rolePermissionService;
        this.userAdminService = userAdminService;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Fail fast on a weak, explicitly-configured password under a prod
        // profile — regardless of whether the user already exists, so a
        // misconfiguration surfaces at startup rather than silently.
        String configured = props.getAdminPassword();
        boolean generatePassword = configured == null || configured.isBlank();
        if (!generatePassword
                && props.isFailOnDefaultPasswordInProd()
                && isProductionProfile()
                && WEAK_PASSWORDS.contains(configured.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "devslab-kit bootstrap refuses to start: a well-known weak password is configured "
                            + "for the bootstrap admin under a production profile. Set "
                            + "devslab.kit.bootstrap.admin-password to a strong value (or leave it blank "
                            + "for a random one), or disable this pin with "
                            + "devslab.kit.bootstrap.fail-on-default-password-in-prod=false.");
        }

        TenantId tenant = ensureTenant();
        RoleId adminRole = ensureAdminRole(tenant);
        ensureAdminPermissionsAndGrants(adminRole);

        UserId adminUser = userRepository
                .findByTenantIdAndLoginId(tenant.value(), props.getAdminLoginId())
                .map(entity -> {
                    log.info("[devslab-kit bootstrap] admin user '{}' already exists in tenant '{}' — skipping create",
                            props.getAdminLoginId(), tenant.value());
                    return UserId.of(entity.getId());
                })
                .orElseGet(() -> createAdminUser(tenant, generatePassword ? generateRandomPassword() : configured, generatePassword));

        userRoleService.assign(adminUser, adminRole, tenant); // idempotent

        seedDeclaredRbac(tenant);

        log.info("[devslab-kit bootstrap] complete: tenant={} role={} user={}",
                tenant.value(), props.getRoleCode(), props.getAdminLoginId());
    }

    private TenantId ensureTenant() {
        TenantId id = TenantId.of(props.getTenantId());
        if (tenantService.findById(id).isPresent()) {
            return id;
        }
        TenantMetadata created = tenantService.create(id, "Default Tenant", TenantMode.SINGLE);
        log.info("[devslab-kit bootstrap] created tenant {}", created.id().value());
        return id;
    }

    private RoleId ensureAdminRole(TenantId tenant) {
        return ensureRoleId(tenant, props.getRoleCode(), props.getRoleName());
    }

    private RoleId ensureRoleId(TenantId tenant, String code, String name) {
        return roleRepository.findByTenantIdAndCode(tenant.value(), code)
                .map(entity -> RoleId.of(entity.getId()))
                .orElseGet(() -> {
                    var role = roleAdminService.create(tenant, code, name);
                    log.info("[devslab-kit bootstrap] created role {}", code);
                    return role.id();
                });
    }

    private void ensureAdminPermissionsAndGrants(RoleId adminRole) {
        for (PermissionSeed perm : ADMIN_PERMISSIONS) {
            rolePermissionService.grant(adminRole, ensurePermissionId(perm.code(), perm.description())); // idempotent
        }
    }

    private PermissionId ensurePermissionId(String code, String description) {
        return permissionRepository.findByCode(code)
                .map(entity -> PermissionId.of(entity.getId()))
                .orElseGet(() -> {
                    permissionAdminService.create(code, description);
                    log.info("[devslab-kit bootstrap] created permission {}", code);
                    return permissionRepository.findByCode(code)
                            .map(e -> PermissionId.of(e.getId()))
                            .orElseThrow();
                });
    }

    /**
     * Idempotently apply the declarative RBAC seed (permissions + roles + grants)
     * from {@code devslab.kit.bootstrap.seed}. Additive only: missing entities are
     * created and listed grants added; nothing is revoked or deleted. A permission
     * referenced by a seeded role is auto-created if absent. Roles live in the
     * bootstrap tenant; permissions are global.
     */
    private void seedDeclaredRbac(TenantId tenant) {
        DevslabKitProperties.Bootstrap.Seed seed = props.getSeed();

        for (String code : seed.getPermissions()) {
            if (code != null && !code.isBlank()) {
                ensurePermissionId(code.trim(), "");
            }
        }

        for (Map.Entry<String, List<String>> entry : seed.getRoles().entrySet()) {
            String roleCode = entry.getKey();
            if (roleCode == null || roleCode.isBlank()) {
                continue;
            }
            RoleId role = ensureRoleId(tenant, roleCode.trim(), roleCode.trim());
            List<String> grants = entry.getValue() == null ? List.of() : entry.getValue();
            for (String permCode : grants) {
                if (permCode != null && !permCode.isBlank()) {
                    rolePermissionService.grant(role, ensurePermissionId(permCode.trim(), "")); // idempotent
                }
            }
        }

        if (!seed.getPermissions().isEmpty() || !seed.getRoles().isEmpty()) {
            log.info("[devslab-kit bootstrap] seeded RBAC: {} permission(s), {} role(s)",
                    seed.getPermissions().size(), seed.getRoles().size());
        }
    }

    private UserId createAdminUser(TenantId tenant, String password, boolean generated) {
        var view = userAdminService.create(
                tenant,
                props.getAdminLoginId(),
                props.getAdminEmail(),
                password,
                "LOCAL",
                props.isMustChangePassword()
        );
        if (generated) {
            logGeneratedPassword(tenant, password);
        } else {
            log.info("[devslab-kit bootstrap] created admin user '{}' in tenant '{}' (password from config, mustChangePassword={})",
                    props.getAdminLoginId(), tenant.value(), props.isMustChangePassword());
        }
        return view.id();
    }

    private void logGeneratedPassword(TenantId tenant, String password) {
        log.warn("""

                ============================================================
                 devslab-kit bootstrap: created first admin
                   tenant : {}
                   login  : {}
                   password (shown ONCE — copy it now): {}
                   mustChangePassword: {}
                ============================================================""",
                tenant.value(), props.getAdminLoginId(), password, props.isMustChangePassword());
    }

    private boolean isProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            String p = profile.toLowerCase(Locale.ROOT);
            if (p.equals("prod") || p.equals("production")) {
                return true;
            }
        }
        return false;
    }

    private static String generateRandomPassword() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private record PermissionSeed(String code, String description) {
    }
}
