package kr.devslab.kit.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 255) String loginId,
        @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 255) String rawPassword,
        @Size(max = 32) String providerType
) {
}
