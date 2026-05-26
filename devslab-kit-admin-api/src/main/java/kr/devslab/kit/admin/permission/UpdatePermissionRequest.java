package kr.devslab.kit.admin.permission;

import jakarta.validation.constraints.Size;

public record UpdatePermissionRequest(@Size(max = 512) String description) {
}
