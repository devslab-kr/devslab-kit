package kr.devslab.kit.admin.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 255) String newRawPassword) {
}
