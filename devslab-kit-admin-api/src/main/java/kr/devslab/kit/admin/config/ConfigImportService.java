package kr.devslab.kit.admin.config;

import java.util.ArrayList;
import java.util.HashMap;
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
import kr.devslab.kit.admin.config.ConfigBundle.MenuDef;
import kr.devslab.kit.admin.config.ConfigBundle.PermissionDef;
import kr.devslab.kit.admin.config.ConfigBundle.RoleDef;
import kr.devslab.kit.core.id.MenuId;
import kr.devslab.kit.core.id.PermissionId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.service.MenuAdminService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a {@link ConfigBundle} to the live database, matching by natural code
 * (ADR 0003). Prototype scope: {@code merge} mode — additive, idempotent upsert that
 * never deletes. {@code dryRun} computes the diff without writing anything. The whole
 * apply runs in one transaction.
 *
 * <p>Menus are processed parent-before-child (the parent must exist to create a child;
 * a menu's parent cannot be changed by update), so a bundle whose menus are in any order
 * still imports correctly.
 */
public class ConfigImportService {

    /** Placeholder id for a not-yet-created menu during a dry-run (never persisted). */
    private static final UUID DRY_RUN_ID = new UUID(0L, 0L);

    private final PermissionAdminService permissions;
    private final RoleAdminService roles;
    private final RolePermissionService rolePermissions;
    private final MenuAdminService menus;

    public ConfigImportService(
            PermissionAdminService permissions,
            RoleAdminService roles,
            RolePermissionService rolePermissions,
            MenuAdminService menus
    ) {
        this.permissions = permissions;
        this.roles = roles;
        this.rolePermissions = rolePermissions;
        this.menus = menus;
    }

    @Transactional
    public ImportResult apply(ConfigBundle bundle, String mode, boolean dryRun) {
        TenantId tenant = TenantId.of(bundle.tenantId());

        ImportResult.Section permSection = importPermissions(bundle.permissions(), dryRun);
        ImportResult.Section roleSection = importRoles(tenant, bundle.roles(), dryRun);
        ImportResult.Section menuSection = importMenus(tenant, bundle.menus(), dryRun);

        return new ImportResult(dryRun, "merge", permSection, roleSection, menuSection);
    }

    private ImportResult.Section importPermissions(List<PermissionDef> defs, boolean dryRun) {
        Map<String, PlatformPermissionEntity> byCode = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getCode, Function.identity()));
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        for (PermissionDef def : defs) {
            PlatformPermissionEntity existing = byCode.get(def.code());
            if (existing == null) {
                created.add(def.code());
                if (!dryRun) {
                    permissions.create(def.code(), def.description());
                }
            } else if (!Objects.equals(existing.getDescription(), def.description())) {
                updated.add(def.code());
                if (!dryRun) {
                    permissions.updateDescription(PermissionId.of(existing.getId()), def.description());
                }
            }
        }
        return ImportResult.Section.of(created, updated);
    }

    private ImportResult.Section importRoles(TenantId tenant, List<RoleDef> defs, boolean dryRun) {
        Map<String, Role> roleByCode = roles.listByTenant(tenant).stream()
                .collect(Collectors.toMap(Role::code, Function.identity()));
        // id -> code over the permissions visible before this run (enough to read a role's current grants)
        Map<UUID, String> permCodeById = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getId, PlatformPermissionEntity::getCode));
        // code -> id for granting (after permission creates; in dry-run only pre-existing ones have ids)
        Map<String, UUID> permIdByCode = permissions.listAll().stream()
                .collect(Collectors.toMap(PlatformPermissionEntity::getCode, PlatformPermissionEntity::getId));

        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        for (RoleDef def : defs) {
            Role existing = roleByCode.get(def.code());
            boolean isNew = existing == null;
            Set<String> currentCodes = isNew ? Set.of() : currentPermissionCodes(existing, permCodeById);
            List<String> toGrant = def.permissionCodes().stream()
                    .filter(code -> !currentCodes.contains(code))
                    .toList();
            boolean renamed = !isNew && !Objects.equals(existing.name(), def.name());

            if (isNew) {
                created.add(def.code());
            } else if (renamed || !toGrant.isEmpty()) {
                updated.add(def.code());
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
            }
        }
        return ImportResult.Section.of(created, updated);
    }

    private Set<String> currentPermissionCodes(Role role, Map<UUID, String> permCodeById) {
        return rolePermissions.findPermissionIdsForRole(role.id()).stream()
                .map(id -> permCodeById.get(id.value()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ImportResult.Section importMenus(TenantId tenant, List<MenuDef> defs, boolean dryRun) {
        Map<String, PlatformMenuEntity> byCode = menus.listByTenant(tenant).stream()
                .collect(Collectors.toMap(PlatformMenuEntity::getCode, Function.identity()));
        Map<String, UUID> idByCode = new HashMap<>();
        byCode.forEach((code, entity) -> idByCode.put(code, entity.getId()));

        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
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
                    created.add(def.code());
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
                        updated.add(def.code());
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
        return ImportResult.Section.of(created, updated);
    }

    private boolean menuChanged(PlatformMenuEntity e, MenuDef def) {
        return !Objects.equals(e.getLabel(), def.label())
                || !Objects.equals(e.getPath(), def.path())
                || !Objects.equals(e.getIcon(), def.icon())
                || !Objects.equals(e.getRequiredPermissionCode(), def.requiredPermissionCode())
                || e.getSortOrder() != def.displayOrder();
    }
}
