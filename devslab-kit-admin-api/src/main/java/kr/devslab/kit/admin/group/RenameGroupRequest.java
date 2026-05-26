package kr.devslab.kit.admin.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameGroupRequest(@NotBlank @Size(max = 128) String name) {
}
