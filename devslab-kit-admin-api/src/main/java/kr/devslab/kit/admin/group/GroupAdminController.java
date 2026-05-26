package kr.devslab.kit.admin.group;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.Group;
import kr.devslab.kit.access.core.service.GroupMembershipService;
import kr.devslab.kit.access.core.service.GroupRoleService;
import kr.devslab.kit.access.core.service.GroupService;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.GroupId;
import kr.devslab.kit.core.id.RoleId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.GROUPS)
public class GroupAdminController {

    private final GroupService groupService;
    private final GroupMembershipService membershipService;
    private final GroupRoleService groupRoleService;

    public GroupAdminController(
            GroupService groupService,
            GroupMembershipService membershipService,
            GroupRoleService groupRoleService
    ) {
        this.groupService = groupService;
        this.membershipService = membershipService;
        this.groupRoleService = groupRoleService;
    }

    @PostMapping
    public ResponseEntity<Group> create(@Valid @RequestBody CreateGroupRequest req) {
        Group group = groupService.create(
                TenantId.of(req.tenantId()),
                req.code(),
                req.name(),
                req.parentGroupId() == null ? null : GroupId.of(req.parentGroupId())
        );
        return ResponseEntity.status(201).body(group);
    }

    @GetMapping
    public List<Group> list(@RequestParam String tenantId) {
        return groupService.findAllByTenant(TenantId.of(tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> rename(@PathVariable UUID id, @Valid @RequestBody RenameGroupRequest req) {
        groupService.rename(GroupId.of(id), req.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        groupService.delete(GroupId.of(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> addMember(@PathVariable UUID id, @PathVariable UUID userId) {
        membershipService.addMember(UserId.of(userId), GroupId.of(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        membershipService.removeMember(UserId.of(userId), GroupId.of(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public List<UserId> members(@PathVariable UUID id) {
        return membershipService.findUsersInGroup(GroupId.of(id));
    }

    @PostMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> assignRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        groupRoleService.assign(GroupId.of(id), RoleId.of(roleId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(@PathVariable UUID id, @PathVariable UUID roleId) {
        groupRoleService.revoke(GroupId.of(id), RoleId.of(roleId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    public List<RoleId> roles(@PathVariable UUID id) {
        return groupRoleService.findRoleIdsForGroup(GroupId.of(id));
    }
}
