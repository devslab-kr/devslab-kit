package kr.devslab.kit.admin.user;

import jakarta.validation.constraints.NotNull;
import kr.devslab.kit.identity.UserStatus;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
