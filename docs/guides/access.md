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
"…*for this specific resource, right now?*". You implement one or more **`Policy`**
beans; the kit's `DefaultPolicyEvaluator` collects every `Policy` bean and dispatches
by its `name()`. (If no policy is registered for a name, evaluation returns
`NOT_APPLICABLE`.)

```java
import java.util.Map;
import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import org.springframework.stereotype.Component;

@Component
class DocOwnerPolicy implements Policy {

    @Override public String name() { return "doc-owner"; }

    @Override
    public PolicyDecision evaluate(PolicyContext ctx) {
        // ctx exposes: userId(), tenantId(), resourceType(), resourceId(),
        // resourceAttributes(), environmentAttributes()
        Object owner = ctx.resourceAttributes().get("ownerLoginId");
        return owner != null /* && owner equals the current user */
                ? PolicyDecision.PERMIT
                : PolicyDecision.DENY;
    }
}
```

Then gate with the ABAC-aware overload of `check`, building the context with the builder:

```java
PolicyContext ctx = PolicyContext.builder()
        .user(userId)
        .tenant(tenantId)
        .resource("doc", docId)
        .resourceAttributes(Map.of("ownerLoginId", doc.ownerLoginId()))
        .build();

access.check(Permission.of("doc.read"), "doc-owner", ctx);
```

For a richer answer (a reason + which rules matched, surfaced by the test endpoint),
override `evaluateDetailed(PolicyContext)` and return a `PolicyEvaluation` —
e.g. `PolicyEvaluation.deny("not the owner", List.of("ownership"))`.

You can dry-run a `(subject, action, resource)` tuple without side effects via the
admin API's `policies` endpoint — see [Admin REST API](../reference/admin-api.md).

See the [Configuration reference](../reference/configuration.md#identity) for the
related JWT and lockout settings.
