package kr.devslab.kit.admin.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameRoleRequest(@NotBlank @Size(max = 128) String name) {
}
