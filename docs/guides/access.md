# Access (RBAC + ABAC)

Authorization in `devslab-kit` is two layers:

1. **RBAC** — users hold **roles** (directly or via **groups**); roles grant
   **permissions** (stable string codes like `admin.user.read`).
2. **ABAC** — an optional **policy** layer that refines an RBAC decision with
   attributes (subject, action, resource, environment).

## Checking permissions

Inject `PermissionChecker`. It evaluates against the current user:

```java
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.Permission;

@Service
class UserAdminService {
    private final PermissionChecker access;

    UserAdminService(PermissionChecker access) { this.access = access; }

    void deactivate(String loginId) {
        access.check(Permission.of("admin.user.write"));   // throws PermissionDeniedException if missing
        // …
    }
}
```

Also available: `hasPermission(Permission)`, `hasAnyPermission(Permission...)`,
`hasAllPermissions(Permission...)`.

## Groups

A **group** bundles roles for a set of users — assign a user to `eng-team` once
instead of attaching every role individually. A user's effective permissions are
the union of their direct roles and their groups' roles.

## ABAC policies

RBAC answers "does this user hold the permission?". ABAC answers the finer
"…*for this specific resource, right now?*". Implement the `PolicyEvaluator` SPI
(the default permits everything) and the kit consults it:

```java
import kr.devslab.kit.access.policy.*;

@Bean
PolicyEvaluator policyEvaluator() {
    return new PolicyEvaluator() {
        @Override public PolicyDecision evaluate(PolicyContext ctx) {
            // inspect ctx.action(), ctx.resourceType(), ctx.subjectAttributes(),
            // ctx.environment() … and return a permit / deny PolicyDecision
            ...
        }
        @Override public List<Policy> policies() { return List.of(); }
    };
}
```

Then gate with the ABAC-aware overload:

```java
access.check(Permission.of("doc.read"), "doc-owner-policy",
    new PolicyContext(userId, tenantId, Map.of(), "read", "doc", docId, Map.of(), Map.of()));
```

You can dry-run a `(subject, action, resource)` tuple without side effects via the
admin API's `policies` endpoint — see [Admin REST API](../reference/admin-api.md).

See the [Configuration reference](../reference/configuration.md#identity) for the
related JWT and lockout settings.
