package kr.devslab.kit.sample;

import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent first-boot seed for the sample app.
 *
 * <p>Provisions just enough for the admin UI to be usable end-to-end against
 * this backend:
 *
 * <ul>
 *   <li>One tenant: {@code default}.</li>
 *   <li>One role: {@code PLATFORM_ADMIN} bound to that tenant.</li>
 *   <li>The {@code admin.*} permission set covering every admin-api endpoint.</li>
 *   <li>One user: {@code admin / admin} (override via {@code SAMPLE_SEED_ADMIN_*}),
 *       holding {@code PLATFORM_ADMIN}.</li>
 * </ul>
 *
 * <p>Every step is idempotent — repeated boots find each entity by code /
 * loginId and skip the create, so re-running against an already-seeded
 * database is a no-op.
 *
 * <p>Disable with {@code sample.seed.enabled=false} for production-like
 * runs where seed data would be unwanted.
 */
public class SampleSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleSeedRunner.class);

    private static final String DEFAULT_TENANT_ID = "default";
    private static final String ADMIN_ROLE_CODE = "PLATFORM_ADMIN";
    private static final String ADMIN_ROLE_NAME = "Platform Admin";

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

    private final SampleSeedProperties props;
    private final TenantService tenantService;
    private final PermissionAdminService permissionAdminService;
    private final JpaPlatformPermissionRepository permissionRepository;
    private final RoleAdminService roleAdminService;
    private final JpaPlatformRoleRepository roleRepository;
    private final RolePermissionService rolePermissionService;
    private final PlatformUserAccountAdminService userAdminService;
    private final JpaPlatformUserAccountRepository userRepository;
    private final UserRoleService userRoleService;

    public SampleSeedRunner(
            SampleSeedProperties props,
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
        if (!props.isEnabled()) {
            log.info("[sample-seed] disabled via sample.seed.enabled=false — skipping");
            return;
        }

        TenantId tenant = ensureTenant();
        RoleId adminRole = ensureAdminRole(tenant);
        ensureAdminPermissionsAndGrants(adminRole);
        UserId adminUser = ensureAdminUser(tenant);
        userRoleService.assign(adminUser, adminRole, tenant);  // idempotent

        log.info("[sample-seed] complete: tenant={} role={} user={} (login as {} / {})",
                tenant.value(), ADMIN_ROLE_CODE, adminUser.value(),
                props.getAdminLoginId(), props.getAdminPassword());
    }

    private TenantId ensureTenant() {
        TenantId id = TenantId.of(DEFAULT_TENANT_ID);
        if (tenantService.findById(id).isPresent()) {
            return id;
        }
        TenantMetadata created = tenantService.create(id, "Default Tenant", TenantMode.SINGLE);
        log.info("[sample-seed] created tenant {}", created.id().value());
        return id;
    }

    private RoleId ensureAdminRole(TenantId tenant) {
        return roleRepository.findByTenantIdAndCode(tenant.value(), ADMIN_ROLE_CODE)
                .map(entity -> RoleId.of(entity.getId()))
                .orElseGet(() -> {
                    var role = roleAdminService.create(tenant, ADMIN_ROLE_CODE, ADMIN_ROLE_NAME);
                    log.info("[sample-seed] created role {}", ADMIN_ROLE_CODE);
                    return role.id();
                });
    }

    private void ensureAdminPermissionsAndGrants(RoleId adminRole) {
        for (PermissionSeed perm : ADMIN_PERMISSIONS) {
            PermissionId permId = permissionRepository.findByCode(perm.code())
                    .map(entity -> PermissionId.of(entity.getId()))
                    .orElseGet(() -> {
                        permissionAdminService.create(perm.code(), perm.description());
                        log.info("[sample-seed] created permission {}", perm.code());
                        return permissionRepository.findByCode(perm.code())
                                .map(e -> PermissionId.of(e.getId()))
                                .orElseThrow();
                    });
            rolePermissionService.grant(adminRole, permId);  // idempotent
        }
    }

    private UserId ensureAdminUser(TenantId tenant) {
        return userRepository.findByTenantIdAndLoginId(tenant.value(), props.getAdminLoginId())
                .map(entity -> UserId.of(entity.getId()))
                .orElseGet(() -> {
                    var view = userAdminService.create(
                            tenant,
                            props.getAdminLoginId(),
                            props.getAdminEmail(),
                            props.getAdminPassword(),
                            "LOCAL"
                    );
                    log.info("[sample-seed] created user {}", view.loginId());
                    return view.id();
                });
    }

    private record PermissionSeed(String code, String description) {
    }
}
