package kr.devslab.kit.admin.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.UserAccountView;
import kr.devslab.kit.identity.UserStatus;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a {@link ConfigBundle} to the live database, matching by natural code (ADR 0003).
 * {@code dryRun} computes the diff without writing anything; the whole apply runs in one
 * transaction.
 *
 * <p><strong>merge</strong> (the default) is additive: it creates and updates, never deletes,
 * and never revokes a role's existing permission grants.
 *
 * <p><strong>mirror</strong> makes the target match the bundle exactly: in addition to the
 * merge, it reconciles each role's grants (revoking permissions not in the bundle) and
 * <em>deletes</em> definitional entities absent from the bundle. There are no FK cascades
 * between roles/permissions/users, so deletes clean their own join rows:
 * <ul>
 *   <li><b>menus</b> — deleted leaf-first (a child before its parent);</li>
 *   <li><b>roles</b> — a role still assigned to any user is <em>skipped</em> (mirror never
 *       strips a user's role); otherwise its permission grants are revoked, then it is deleted;</li>
 *   <li><b>permissions</b> — revoked from this tenant's roles, then deleted. Note permissions
 *       are global (not tenant-scoped): mirror is intended for single-tenant-per-deployment use
 *       and is dev/staging-only (it is refused under a production profile).</li>
 * </ul>
 *
 * <p>Menus are processed parent-before-child on create so a bundle whose menus are in any
 * order still imports correctly.
 *
 * <p>Users are only touched when {@code includeUsers} is set, and only ever created — an
 * existing user is left untouched ({@code skipped}) and no password is ever carried: a
 * created user has no usable password and must have one set by an admin.
 */
public class ConfigImportService {

    /** Placeholder id for a not-yet-created menu during a dry-run (never persisted). */
    private static final UUID DRY_RUN_ID = new UUID(0L, 0L);

    private final PermissionAdminService permissions;
    private final RoleAdminService roles;
    private final RolePermissionService rolePermissions;
    private final MenuAdminService menus;
    private final PlatformUserAccountAdminService userAccounts;
    private final UserRoleService userRoles;

    public ConfigImportService(
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

    /** Definitional-only apply (no user sync). */
    @Transactional
    public ImportResult apply(ConfigBundle bundle, String mode, boolean dryRun) {
        return apply(bundle, mode, dryRun, false);
    }

    @Transactional
    public ImportResult apply(ConfigBundle bundle, String mode, boolean dryRun, boolean includeUsers) {
        boolean mirror = "mirror".equalsIgnoreCase(mode);
        TenantId tenant = TenantId.of(bundle.tenantId());

        Upsert perms = importPermissions(bundle.permissions(), dryRun);
        Upsert roleUpsert = importRoles(tenant, bundle.roles(), dryRun, mirror);
        Upsert menuUpsert = importMenus(tenant, bundle.menus(), dryRun);
        ImportResult.Section users = includeUsers
                ? importUsers(tenant, bundle.users(), dryRun)
                : ImportResult.Section.EMPTY;

        Deletions menuDel = Deletions.EMPTY;
        Deletions roleDel = Deletions.EMPTY;
        Deletions permDel = Deletions.EMPTY;
        if (mirror) {
            // Order matters with no FK cascades: menus, then roles, then permissions.
            menuDel = deleteMenusNotIn(tenant, codes(bundle.menus(), MenuDef::code), dryRun);
            roleDel = deleteRolesNotIn(tenant, codes(bundle.roles(), RoleDef::code), dryRun);
            permDel = deletePermissionsNotIn(tenant, codes(bundle.permissions(), PermissionDef::code), dryRun);
        }

        return new ImportResult(
                dryRun,
                mirror ? "mirror" : "merge",
                ImportResult.Section.of(perms.created, perms.updated, permDel.deleted, permDel.skipped),
                ImportResult.Section.of(roleUpsert.created, roleUpsert.updated, roleDel.deleted, roleDel.skipped),
                ImportResult.Section.of(menuUpsert.created, menuUpsert.updated, menuDel.deleted, menuDel.skipped),
                users);
    }

    // ── Upsert (create / update) ────────────────────────────────────────────

    private Upsert importPermissions(List<PermissionDef> defs, boolean dryRun) {
        Map<String, PlatformPermissionEntity> byCode = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getCode, Function.identity()));
        Upsert out = new Upsert();
        for (PermissionDef def : defs) {
            PlatformPermissionEntity existing = byCode.get(def.code());
            if (existing == null) {
                out.created.add(def.code());
                if (!dryRun) {
                    permissions.create(def.code(), def.description());
                }
            } else if (!Objects.equals(existing.getDescription(), def.description())) {
                out.updated.add(def.code());
                if (!dryRun) {
                    permissions.updateDescription(PermissionId.of(existing.getId()), def.description());
                }
            }
        }
        return out;
    }

    private Upsert importRoles(TenantId tenant, List<RoleDef> defs, boolean dryRun, boolean mirror) {
        Map<String, Role> roleByCode = roles.listByTenant(tenant).stream()
                .collect(Collectors.toMap(Role::code, Function.identity()));
        Map<UUID, String> permCodeById = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getId, PlatformPermissionEntity::getCode));
        Map<String, UUID> permIdByCode = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getCode, PlatformPermissionEntity::getId));

        Upsert out = new Upsert();
        for (RoleDef def : defs) {
            Role existing = roleByCode.get(def.code());
            boolean isNew = existing == null;
            Set<String> currentCodes = isNew ? Set.of() : currentPermissionCodes(existing, permCodeById);
            Set<String> wantCodes = new HashSet<>(def.permissionCodes());
            List<String> toGrant = def.permissionCodes().stream()
                    .filter(code -> !currentCodes.contains(code))
                    .toList();
            // mirror also tightens grants: revoke anything the role has that the bundle doesn't.
            List<String> toRevoke = mirror
                    ? currentCodes.stream().filter(code -> !wantCodes.contains(code)).toList()
                    : List.of();
            boolean renamed = !isNew && !Objects.equals(existing.name(), def.name());

            if (isNew) {
                out.created.add(def.code());
            } else if (renamed || !toGrant.isEmpty() || !toRevoke.isEmpty()) {
                out.updated.add(def.code());
            }

            if (!dryRun) {
                Role role = isNew ? roles.create(tenant, def.code(), def.name()) : existing;
                if (renamed) {
                    roles.rename(role.id(), def.name());
                }
                for (String code : toGrant) {
                    UUID permId = permIdByCode.get(code);
                    if (permId != null) {
                        rolePermissions.grant(role.id(), PermissionId.of(permId));
                    }
                }
                for (String code : toRevoke) {
                    UUID permId = permIdByCode.get(code);
                    if (permId != null) {
                        rolePermissions.revoke(role.id(), PermissionId.of(permId));
                    }
                }
            }
        }
        return out;
    }

    private Set<String> currentPermissionCodes(Role role, Map<UUID, String> permCodeById) {
        return rolePermissions.findPermissionIdsForRole(role.id()).stream()
                .map(id -> permCodeById.get(id.value()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Upsert importMenus(TenantId tenant, List<MenuDef> defs, boolean dryRun) {
        Map<String, PlatformMenuEntity> byCode = menus.listByTenant(tenant).stream()
                .collect(Collectors.toMap(PlatformMenuEntity::getCode, Function.identity()));
        Map<String, UUID> idByCode = new HashMap<>();
        byCode.forEach((code, entity) -> idByCode.put(code, entity.getId()));

        Upsert out = new Upsert();
        List<MenuDef> pending = new ArrayList<>(defs);

        boolean progress = true;
        while (!pending.isEmpty() && progress) {
            progress = false;
            Iterator<MenuDef> it = pending.iterator();
            while (it.hasNext()) {
                MenuDef def = it.next();
                boolean parentReady = def.parentCode() == null || idByCode.containsKey(def.parentCode());
                if (!parentReady) {
                    continue;
                }
                PlatformMenuEntity existing = byCode.get(def.code());
                if (existing == null) {
                    out.created.add(def.code());
                    if (dryRun) {
                        idByCode.put(def.code(), DRY_RUN_ID);
                    } else {
                        UUID parentId = def.parentCode() == null ? null : idByCode.get(def.parentCode());
                        PlatformMenuEntity createdEntity = menus.create(
                                tenant, def.code(), def.label(), def.path(),
                                parentId == null ? null : MenuId.of(parentId),
                                def.displayOrder(), def.requiredPermissionCode(), def.icon());
                        idByCode.put(def.code(), createdEntity.getId());
                    }
                } else {
                    if (menuChanged(existing, def)) {
                        out.updated.add(def.code());
                        if (!dryRun) {
                            menus.update(MenuId.of(existing.getId()), def.label(), def.path(),
                                    def.displayOrder(), def.requiredPermissionCode(), def.icon());
                        }
                    }
                    idByCode.put(def.code(), existing.getId());
                }
                it.remove();
                progress = true;
            }
        }
        // pending leftovers reference a parent that is neither in the DB nor the bundle — skip them.
        return out;
    }

    private boolean menuChanged(PlatformMenuEntity e, MenuDef def) {
        return !Objects.equals(e.getLabel(), def.label())
                || !Objects.equals(e.getPath(), def.path())
                || !Objects.equals(e.getIcon(), def.icon())
                || !Objects.equals(e.getRequiredPermissionCode(), def.requiredPermissionCode())
                || e.getSortOrder() != def.displayOrder();
    }

    // ── User sync (create-only) ──────────────────────────────────────────────

    private ImportResult.Section importUsers(TenantId tenant, List<UserDef> defs, boolean dryRun) {
        Map<String, UserAccountView> byLogin = userAccounts.listByTenant(tenant).stream()
                .collect(Collectors.toMap(UserAccountView::loginId, Function.identity(), (a, b) -> a));
        Map<String, UUID> roleIdByCode = roles.listByTenant(tenant).stream()
                .collect(Collectors.toMap(Role::code, role -> role.id().value()));

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (UserDef def : defs) {
            if (byLogin.containsKey(def.loginId())) {
                skipped.add(def.loginId()); // never overwrite an existing user
                continue;
            }
            created.add(def.loginId());
            if (!dryRun) {
                // No secret is transported: create with no usable password + mustChangePassword.
                UserAccountView user = userAccounts.create(
                        tenant, def.loginId(), def.email(), null, "LOCAL", true);
                for (String roleCode : def.roleCodes()) {
                    UUID roleId = roleIdByCode.get(roleCode);
                    if (roleId != null) {
                        userRoles.assign(user.id(), RoleId.of(roleId), tenant);
                    }
                }
                applyStatus(user.id(), def.status());
            }
        }
        return ImportResult.Section.of(created, List.of(), List.of(), skipped);
    }

    private void applyStatus(UserId id, String status) {
        if (status == null) {
            return;
        }
        try {
            UserStatus parsed = UserStatus.valueOf(status);
            if (parsed != UserStatus.ACTIVE) { // create() already sets ACTIVE
                userAccounts.setStatus(id, parsed);
            }
        } catch (IllegalArgumentException ignored) {
            // unknown status string — leave the account ACTIVE
        }
    }

    // ── Mirror deletes ───────────────────────────────────────────────────────

    private Deletions deleteMenusNotIn(TenantId tenant, Set<String> keep, boolean dryRun) {
        List<PlatformMenuEntity> pending = menus.listByTenant(tenant).stream()
                .filter(m -> !keep.contains(m.getCode()))
                .collect(Collectors.toCollection(ArrayList::new));
        List<String> deleted = new ArrayList<>();
        boolean progress = true;
        while (!pending.isEmpty() && progress) {
            progress = false;
            Iterator<PlatformMenuEntity> it = pending.iterator();
            while (it.hasNext()) {
                PlatformMenuEntity menu = it.next();
                boolean hasPendingChild = pending.stream()
                        .anyMatch(other -> menu.getId().equals(other.getParentId()));
                if (hasPendingChild) {
                    continue; // delete children first
                }
                deleted.add(menu.getCode());
                if (!dryRun) {
                    menus.delete(MenuId.of(menu.getId()));
                }
                it.remove();
                progress = true;
            }
        }
        // any leftover (only possible on a parent cycle, which the schema disallows) — delete anyway
        for (PlatformMenuEntity menu : pending) {
            deleted.add(menu.getCode());
            if (!dryRun) {
                menus.delete(MenuId.of(menu.getId()));
            }
        }
        return new Deletions(deleted, List.of());
    }

    private Deletions deleteRolesNotIn(TenantId tenant, Set<String> keep, boolean dryRun) {
        Set<UUID> assignedRoleIds = new HashSet<>();
        for (UserAccountView user : userAccounts.listByTenant(tenant)) {
            userRoles.findRoleIdsForUser(user.id()).forEach(rid -> assignedRoleIds.add(rid.value()));
        }
        List<String> deleted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Role role : roles.listByTenant(tenant)) {
            if (keep.contains(role.code())) {
                continue;
            }
            if (assignedRoleIds.contains(role.id().value())) {
                skipped.add(role.code()); // still assigned to a user — mirror won't strip it
                continue;
            }
            deleted.add(role.code());
            if (!dryRun) {
                for (PermissionId pid : rolePermissions.findPermissionIdsForRole(role.id())) {
                    rolePermissions.revoke(role.id(), pid);
                }
                roles.delete(role.id());
            }
        }
        return new Deletions(deleted, skipped);
    }

    private Deletions deletePermissionsNotIn(TenantId tenant, Set<String> keep, boolean dryRun) {
        List<Role> tenantRoles = roles.listByTenant(tenant);
        List<String> deleted = new ArrayList<>();
        for (PlatformPermissionEntity perm : permissions.listAll()) {
            if (keep.contains(perm.getCode())) {
                continue;
            }
            deleted.add(perm.getCode());
            if (!dryRun) {
                PermissionId pid = PermissionId.of(perm.getId());
                for (Role role : tenantRoles) {
                    rolePermissions.revoke(role.id(), pid); // idempotent if not granted
                }
                permissions.delete(pid);
            }
        }
        return new Deletions(deleted, List.of());
    }

    private static <T> Set<String> codes(List<T> defs, Function<T, String> code) {
        return defs.stream().map(code).collect(Collectors.toSet());
    }

    /** Mutable accumulator for create/update codes during a single upsert pass. */
    private static final class Upsert {
        final List<String> created = new ArrayList<>();
        final List<String> updated = new ArrayList<>();
    }

    /** Immutable result of a mirror delete pass. */
    private record Deletions(List<String> deleted, List<String> skipped) {
        static final Deletions EMPTY = new Deletions(List.of(), List.of());
    }
}
