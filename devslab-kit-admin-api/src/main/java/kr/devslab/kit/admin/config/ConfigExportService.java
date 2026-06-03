package kr.devslab.kit.admin.config;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.devslab.kit.access.Role;
import kr.devslab.kit.access.core.entity.PlatformPermissionEntity;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.admin.config.ConfigBundle.MenuDef;
import kr.devslab.kit.admin.config.ConfigBundle.PermissionDef;
import kr.devslab.kit.admin.config.ConfigBundle.RoleDef;
import kr.devslab.kit.admin.config.ConfigBundle.UserDef;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.identity.UserAccountView;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.service.MenuAdminService;

/**
 * Builds a {@link ConfigBundle} from the live database for a tenant, translating every
 * DB id into the corresponding natural code so the result is portable across environments
 * (ADR 0003). Read-only.
 *
 * <p>Definitional config (permissions, roles, menus) is always exported. Users are
 * exported only when {@code includeUsers} is set, and even then carry no password —
 * just login id, email, status and assigned role codes.
 */
public class ConfigExportService {

    private final PermissionAdminService permissions;
    private final RoleAdminService roles;
    private final RolePermissionService rolePermissions;
    private final MenuAdminService menus;
    private final PlatformUserAccountAdminService userAccounts;
    private final UserRoleService userRoles;

    public ConfigExportService(
            PermissionAdminService permissions,
            RoleAdminService roles,
            RolePermissionService rolePermissions,
            MenuAdminService menus,
            PlatformUserAccountAdminService userAccounts,
            UserRoleService userRoles
    ) {
        this.permissions = permissions;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.menus = menus;
        this.userAccounts = userAccounts;
        this.userRoles = userRoles;
    }

    /** Definitional-only export (no users). */
    public ConfigBundle export(TenantId tenantId) {
        return export(tenantId, false);
    }

    public ConfigBundle export(TenantId tenantId, boolean includeUsers) {
        List<PlatformPermissionEntity> permEntities = permissions.listAll();
        Map<UUID, String> permIdToCode = permEntities.stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getId, PlatformPermissionEntity::getCode));

        List<PermissionDef> permissionDefs = permEntities.stream()
                .sorted(Comparator.comparing(PlatformPermissionEntity::getCode))
                .map(e -> new PermissionDef(e.getCode(), e.getDescription()))
                .toList();

        List<RoleDef> roleDefs = roles.listByTenant(tenantId).stream()
                .sorted(Comparator.comparing(Role::code))
                .map(role -> new RoleDef(role.code(), role.name(), permissionCodesFor(role, permIdToCode)))
                .toList();

        List<PlatformMenuEntity> menuEntities = menus.listByTenant(tenantId);
        Map<UUID, String> menuIdToCode = menuEntities.stream()
                .collect(Collectors.toMap(PlatformMenuEntity::getId, PlatformMenuEntity::getCode));

        List<MenuDef> menuDefs = menuEntities.stream()
                .sorted(Comparator.comparingInt(PlatformMenuEntity::getSortOrder)
                        .thenComparing(PlatformMenuEntity::getCode))
                .map(menu -> new MenuDef(
                        menu.getCode(),
                        menu.getParentId() == null ? null : menuIdToCode.get(menu.getParentId()),
                        menu.getLabel(),
                        menu.getPath(),
                        menu.getIcon(),
                        menu.getRequiredPermissionCode(),
                        menu.getSortOrder()))
                .toList();

        List<UserDef> userDefs = includeUsers ? exportUsers(tenantId) : List.of();

        return new ConfigBundle(
                ConfigBundle.CURRENT_VERSION, tenantId.value(), permissionDefs, roleDefs, menuDefs, userDefs);
    }

    private List<String> permissionCodesFor(Role role, Map<UUID, String> permIdToCode) {
        return rolePermissions.findPermissionIdsForRole(role.id()).stream()
                .map(id -> permIdToCode.get(id.value()))
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private List<UserDef> exportUsers(TenantId tenantId) {
        Map<UUID, String> roleCodeById = roles.listByTenant(tenantId).stream()
                .collect(Collectors.toMap(role -> role.id().value(), Role::code));
        return userAccounts.listByTenant(tenantId).stream()
                .sorted(Comparator.comparing(UserAccountView::loginId))
                .map(user -> new UserDef(
                        user.loginId(),
                        user.email(),
                        user.status().name(),
                        userRoles.findRoleIdsForUser(user.id()).stream()
                                .map(roleId -> roleCodeById.get(roleId.value()))
                                .filter(Objects::nonNull)
                                .sorted()
                                .toList()))
                .toList();
    }
}
