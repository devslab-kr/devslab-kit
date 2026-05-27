package kr.devslab.kit.admin.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 64) String tenantId,
        @NotBlank @Size(max = 255) String loginId,
        @NotBlank @Size(min = 1, max = 255) String rawPassword
) {
}
