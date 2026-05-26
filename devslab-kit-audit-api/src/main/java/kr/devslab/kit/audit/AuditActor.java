package kr.devslab.kit.audit;

import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;

public record AuditActor(UserId userId, TenantId tenantId, String displayName) {
}
