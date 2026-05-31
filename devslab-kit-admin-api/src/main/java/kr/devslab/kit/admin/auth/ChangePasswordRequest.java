package kr.devslab.kit.admin.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service password change payload. The caller must be authenticated; the
 * account is taken from the security principal, not the request body, so a
 * user can only change their own password.
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 255) String oldPassword,
        @NotBlank @Size(min = 8, max = 255) String newPassword
) {
}
