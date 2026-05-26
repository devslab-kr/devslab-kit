package kr.devslab.kit.access;

import java.util.Objects;
import java.util.Optional;
import kr.devslab.kit.core.id.GroupId;
import kr.devslab.kit.core.id.TenantId;

public record Group(
        GroupId id,
        TenantId tenantId,
        String code,
        String name,
        Optional<GroupId> parentGroupId
) {

    public Group {
        Objects.requireNonNull(id, "Group id must not be null");
        Objects.requireNonNull(tenantId, "Group tenantId must not be null");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Group code must not be null or blank");
        }
        parentGroupId = parentGroupId == null ? Optional.empty() : parentGroupId;
    }
}
