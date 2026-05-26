package kr.devslab.kit.admin.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        UUID parentGroupId
) {
}
