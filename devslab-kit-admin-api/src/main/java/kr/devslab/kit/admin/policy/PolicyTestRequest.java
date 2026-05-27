package kr.devslab.kit.admin.policy;

import java.util.Map;
import java.util.UUID;

public record PolicyTestRequest(
        UUID userId,
        String tenantId,
        String resourceType,
        String resourceId,
        Map<String, Object> resourceAttributes,
        Map<String, Object> environmentAttributes
) {
}
