package kr.devslab.kit.access.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "platform_user_group",
        uniqueConstraints = @UniqueConstraint(name = "uq_platform_user_group_user_group", columnNames = {"user_id", "group_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformUserGroupEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "group_id", columnDefinition = "uuid", nullable = false)
    private UUID groupId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public PlatformUserGroupEntity(UUID id, UUID userId, UUID groupId, Instant joinedAt) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.joinedAt = joinedAt;
    }
}
