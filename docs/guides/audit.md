# Audit Logging

An **audit trail** is a permanent record of who did what, when, and whether it
worked — "user `admin` suspended tenant `acme` at 14:03, success". The kit records
this trail **asynchronously**, off the request's critical path, and persists each
event to PostgreSQL with its metadata as JSONB.

New here? Do the [Tutorial](../getting-started/tutorial.md) first. This guide assumes
you have a running app.

## How it flows

```
your code                    kit                              PostgreSQL
─────────                    ───                              ──────────
audit.publish(event)  ──►  publish() hands it to an    ──►   one row in the
                           async executor (off the           audit table, metadata
                           request thread)                   stored as JSONB
```

Because the write is async, a slow or failing audit write never slows down — or
fails — the request that triggered it.

## Emit an event

Inject `AuditEventPublisher` and `publish` an `AuditEvent`. Build the event with
`AuditEvent.builder()`:

```java
// src/main/java/com/example/myapp/TenantAdminService.java
import java.time.Instant;
import java.util.Map;
import kr.devslab.kit.audit.AuditAction;
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.audit.AuditOutcome;
import kr.devslab.kit.audit.AuditTarget;

@Service
class TenantAdminService {

    private final AuditEventPublisher audit;

    TenantAdminService(AuditEventPublisher audit) { this.audit = audit; }

    void suspend(String tenantId, String reason) {
        // … perform the change …

        audit.publish(AuditEvent.builder()
                .action(AuditAction.of("tenant.suspend"))      // required
                .target(new AuditTarget("tenant", tenantId))
                .outcome(AuditOutcome.SUCCESS)                 // SUCCESS | FAILURE
                .occurredAt(Instant.now())                     // required
                .metadata(Map.of("reason", reason))            // free-form → JSONB
                .build());
    }
}
```

### The fields

| Field | Required | What it is |
| --- | --- | --- |
| `action` | **yes** | What happened, as a stable code: `AuditAction.of("tenant.suspend")`. |
| `occurredAt` | **yes** | When — `Instant.now()`. |
| `target` | no | What was acted on: `new AuditTarget(type, id)`, e.g. `("tenant", "acme")`. |
| `outcome` | no | `AuditOutcome.SUCCESS` or `AuditOutcome.FAILURE`. |
| `actor` | no | Who did it: `new AuditActor(userId, tenantId, displayName)`. |
| `metadata` | no | Any extra context as a `Map<String,Object>` — stored as JSONB. |
| `ip` / `userAgent` | no | Request origin, when you have it. |

!!! warning "`occurredAt` is required, `actor` is not auto-filled"
    `build()` throws if `occurredAt` is missing — always set it. `actor` is optional,
    but the kit does **not** fill it from the security context for you: if you want
    the acting user recorded, set `actor` explicitly (e.g. from your `CurrentUser`).

## Read the trail

=== "Admin console"

    Open the [admin console](admin-console.md) → **Audit Logs**: a searchable,
    lazily-paginated table with filters (actor / action / target type / outcome /
    time range) and a JSON-payload detail drawer. See the
    [Admin Console guide → Audit Logs](admin-console.md#audit-logs).

=== "REST API"

    ```bash
    # filter by action + outcome + time range:
    curl -G localhost:8080/admin/api/v1/audit-logs \
      -H 'Authorization: Bearer <token>' \
      --data-urlencode 'tenantId=default' \
      --data-urlencode 'action=tenant.suspend' \
      --data-urlencode 'outcome=FAILURE' \
      --data-urlencode 'from=2026-06-01T00:00:00Z'
    ```

    Returns a paged list. See the [Admin REST API](../reference/admin-api.md) for the
    full `audit-logs` query parameters.

## Tuning

| Key | Default | |
| --- | --- | --- |
| `audit.enabled` | `true` | Turn the whole subsystem off if you don't need it. |
| `audit.async-queue-capacity` | `1024` | Bounded queue feeding the async writer. |

The queue is **bounded** on purpose — under a flood, audit writes shed rather than
exhaust memory. Size it for your throughput. See the
[Configuration reference](../reference/configuration.md#audit).

## See also

- [Admin Console → Audit Logs](admin-console.md#audit-logs) — the searchable viewer.
- [Admin REST API](../reference/admin-api.md) — the `audit-logs` resource.
