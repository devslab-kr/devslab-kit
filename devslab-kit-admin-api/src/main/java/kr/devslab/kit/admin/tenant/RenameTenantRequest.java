package kr.devslab.kit.admin.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameTenantRequest(@NotBlank @Size(max = 128) String name) {
}
