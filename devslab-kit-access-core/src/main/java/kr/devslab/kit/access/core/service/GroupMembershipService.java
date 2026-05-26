package kr.devslab.kit.access.core.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.access.core.entity.PlatformUserGroupEntity;
import kr.devslab.kit.access.core.repository.JpaPlatformUserGroupRepository;
import kr.devslab.kit.core.id.GroupId;
import kr.devslab.kit.core.id.UserId;
import org.springframework.transaction.annotation.Transactional;

public class GroupMembershipService {

    private final JpaPlatformUserGroupRepository repository;
    private final Clock clock;

    public GroupMembershipService(JpaPlatformUserGroupRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void addMember(UserId userId, GroupId groupId) {
        if (repository.findByUserIdAndGroupId(userId.value(), groupId.value()).isPresent()) {
            return;
        }
        repository.save(new PlatformUserGroupEntity(
                UUID.randomUUID(),
                userId.value(),
                groupId.value(),
                Instant.now(clock)
        ));
    }

    @Transactional
    public void removeMember(UserId userId, GroupId groupId) {
        repository.deleteByUserIdAndGroupId(userId.value(), groupId.value());
    }

    @Transactional(readOnly = true)
    public List<GroupId> findGroupsForUser(UserId userId) {
        return repository.findAllByUserId(userId.value()).stream()
                .map(entity -> GroupId.of(entity.getGroupId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserId> findUsersInGroup(GroupId groupId) {
        return repository.findAllByGroupId(groupId.value()).stream()
                .map(entity -> UserId.of(entity.getUserId()))
                .toList();
    }
}
