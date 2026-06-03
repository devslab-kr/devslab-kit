# Access (RBAC + ABAC)

**Authorization** decides who may do what. `devslab-kit` does it in two layers:

1. **RBAC** (role-based) — the coarse layer. A **user** holds **roles** (directly or
   via **groups**); a role grants **permissions** — stable string codes like
   `admin.user.read`. "Can this user do X?"
2. **ABAC** (attribute-based) — an optional fine layer that refines an RBAC decision
   with attributes. "Can they do X *to this specific resource, right now*?"

Most apps need only RBAC. Reach for ABAC when a permission depends on the *data*
(owner-only edits, same-tenant-only, business hours). New here? Do the
[Tutorial](../getting-started/tutorial.md) first — Steps 6–9 set this up live.

## The mental model

```
            ┌─ direct roles ─┐
   user ────┤                ├──► roles ──► permissions     ← RBAC: do they hold it?
            └─ groups ─ roles┘
                                        then, optionally:
   permission + resource attributes ──► Policy ──► PERMIT/DENY   ← ABAC: for THIS resource?
```

A user's **effective permissions** are the union of their direct roles' and their
groups' roles' permissions.

## Step 1 — Set up roles & permissions

You define permissions, group them into roles, and assign roles to users. (The
first-admin [bootstrap](bootstrap.md) already seeds `PLATFORM_ADMIN` with the full
`admin.*` set — this is how you add your own.)

!!! tip "Seed them from config instead of clicking"
    To avoid hand-creating starter roles in every environment, declare them under
    `devslab.kit.bootstrap.seed` and the kit creates them idempotently on boot —
    see [First-admin Bootstrap → Seed](bootstrap.md#seed).

=== "Admin console"

    In the [admin console](admin-console.md):

    1. **Permissions** → **New** → add a code like `doc.read` (+ description).
    2. **Roles** → **New** → create e.g. `editor`.
    3. Open the role → **grant** it `doc.read` (and any others).
    4. **Users** → pick a user → **assign** the `editor` role (or add them to a
       **Group** that carries it).

=== "REST API"

    ```bash
    # 1. create a permission
    curl -X POST localhost:8080/admin/api/v1/permissions \
      -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
      -d '{"code":"doc.read","description":"Read documents"}'

    # 2. create a role
    curl -X POST localhost:8080/admin/api/v1/roles \
      -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","code":"editor","name":"Editor"}'

    # 3. grant the permission to the role   (ids from the responses above)
    curl -X POST localhost:8080/admin/api/v1/roles/{roleId}/permissions/{permissionId} \
      -H 'Authorization: Bearer <token>'

    # 4. assign the role to a user
    curl -X POST localhost:8080/admin/api/v1/roles/{roleId}/users/{userId} \
      -H 'Authorization: Bearer <token>'
    ```

    See the [Admin REST API](../reference/admin-api.md) for the full `permissions`,
    `roles` and `groups` resources.

## Step 2 — Check permissions in code

Inject `PermissionChecker`. It evaluates against the current user:

```java
// src/main/java/com/example/myapp/DocService.java
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.Permission;

@Service
class DocService {

    private final PermissionChecker access;

    DocService(PermissionChecker access) { this.access = access; }

    Document open(String docId) {
        access.check(Permission.of("doc.read"));   // throws PermissionDeniedException if missing
        return load(docId);
    }
}
```

Also available: `hasPermission(Permission)`, `hasAnyPermission(Permission...)`,
`hasAllPermissions(Permission...)` — use these when you want to branch rather than
throw.

## Groups

A **group** bundles roles for a set of users — assign a user to `eng-team` once
instead of attaching every role individually. Manage groups (members + role grants)
in the [admin console](admin-console.md) or the `groups` REST resource. A user's
effective permissions include their groups' roles automatically.

## Step 3 — ABAC for per-resource rules { #abac-policies }

RBAC answers "does this user hold the permission?". ABAC answers the finer
"…*for this specific resource, right now?*". You implement one or more **`Policy`**
beans; the kit's `DefaultPolicyEvaluator` collects every `Policy` bean and dispatches
by its `name()`. (If no policy is registered for a name, evaluation returns
`NOT_APPLICABLE`.)

```java
// src/main/java/com/example/myapp/DocOwnerPolicy.java
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

Then gate with the ABAC-aware overload of `check`, building the context with the
builder:

```java
// inside DocService, when editing a specific doc:
PolicyContext ctx = PolicyContext.builder()
        .user(userId)
        .tenant(tenantId)
        .resource("doc", docId)
        .resourceAttributes(Map.of("ownerLoginId", doc.ownerLoginId()))
        .build();

access.check(Permission.of("doc.read"), "doc-owner", ctx);   // RBAC first, then the policy
```

`check` enforces RBAC **and** the named policy: the user must hold `doc.read` *and*
the `doc-owner` policy must `PERMIT`.

For a richer answer (a reason + which rules matched, surfaced by the test endpoint),
override `evaluateDetailed(PolicyContext)` and return a `PolicyEvaluation` —
e.g. `PolicyEvaluation.deny("not the owner", List.of("ownership"))`.

!!! tip "Dry-run a decision"
    The admin console's **Policies** screen (and the `policies` admin endpoint) can
    evaluate a `(subject, action, resource)` tuple **without side effects**, so you
    can test a policy before wiring it into a path. See the
    [Admin Console guide](admin-console.md) and [Admin REST API](../reference/admin-api.md).

## See also

- [Admin Console](admin-console.md) — manage roles, permissions, groups and test policies.
- [Dynamic Menus](menus.md) — show users only the menu items their permissions allow.
- [Configuration reference](../reference/configuration.md#identity) — JWT + lockout settings.
