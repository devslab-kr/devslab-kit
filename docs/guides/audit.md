# Audit Logging

The kit records an audit trail **asynchronously**, so logging never sits on the
request's critical path. Events are persisted to PostgreSQL with their metadata as
JSONB.

## Emitting an event

Inject `AuditEventPublisher` and `publish` an `AuditEvent`. An `AuditEvent` carries
an **actor**, an **action**, a **target**, an **outcome**, a timestamp, and a
free-form **metadata** map; build it with `AuditEvent.builder()`:

```java
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;

@Service
class TenantAdminService {
    private final AuditEventPublisher audit;

    TenantAdminService(AuditEventPublisher audit) { this.audit = audit; }

    void suspend(String tenantId, String reason) {
        // … perform the change …
        audit.publish(
            AuditEvent.builder()
                // action / target / outcome / metadata — see the AuditEvent builder
                .build());
    }
}
```

The actor and timestamp are filled from the current context when omitted; the
publisher hands the event to an async listener that writes it.

## Reading the trail

Search and filter audit logs through the admin API (`audit-logs`) — by actor,
action, target type, outcome and time range. See
[Admin REST API](../reference/admin-api.md). The
[admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) ships a
searchable audit-log view with a detail drawer.

## Tuning

| Key | Default | |
| --- | --- | --- |
| `audit.enabled` | `true` | Turn the whole subsystem off if you don't need it. |
| `audit.async-queue-capacity` | `1024` | Bounded queue feeding the async writer. |

The queue is **bounded** on purpose — under a flood, audit writes shed rather than
exhaust memory. Size it for your throughput. See the
[Configuration reference](../reference/configuration.md#audit).
