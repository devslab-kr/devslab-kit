package kr.devslab.kit.access.policy;

import java.util.Map;
import java.util.Optional;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record PolicyContext(
        Optional<UserId> userId,
        Optional<TenantId> tenantId,
        String resourceType,
        String resourceId,
        Map<String, Object> resourceAttributes,
        Map<String, Object> environmentAttributes
) {

    public PolicyContext {
        userId = userId == null ? Optional.empty() : userId;
        tenantId = tenantId == null ? Optional.empty() : tenantId;
        resourceAttributes = resourceAttributes == null ? Map.of() : Map.copyOf(resourceAttributes);
        environmentAttributes = environmentAttributes == null ? Map.of() : Map.copyOf(environmentAttributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Optional<UserId> userId = Optional.empty();
        private Optional<TenantId> tenantId = Optional.empty();
        private String resourceType;
        private String resourceId;
        private Map<String, Object> resourceAttributes = Map.of();
        private Map<String, Object> environmentAttributes = Map.of();

        public Builder user(UserId id) { this.userId = Optional.ofNullable(id); return this; }
        public Builder tenant(TenantId id) { this.tenantId = Optional.ofNullable(id); return this; }
        public Builder resource(String type, String id) { this.resourceType = type; this.resourceId = id; return this; }
        public Builder resourceAttributes(Map<String, Object> attrs) { this.resourceAttributes = attrs; return this; }
        public Builder environmentAttributes(Map<String, Object> attrs) { this.environmentAttributes = attrs; return this; }
        public PolicyContext build() {
            return new PolicyContext(userId, tenantId, resourceType, resourceId, resourceAttributes, environmentAttributes);
        }
    }
}
