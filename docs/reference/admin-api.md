# Admin REST API

The kit exposes a management API under `/admin/api/v1`. The companion
[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui) is
built directly on it; you can also call it from your own tooling.

| Resource | What it does |
| --- | --- |
| `auth` | Log in (returns a JWT) and change password. |
| `users` | CRUD users, reset password, lock/unlock, change status. |
| `roles` | CRUD roles and assign permissions. |
| `permissions` | CRUD permission definitions. |
| `groups` | CRUD subject groups and manage membership. |
| `menus` | Manage the menu tree (per-item required permission, ordering). |
| `tenants` | CRUD tenants and change tenant status. |
| `policies` | List the registered ABAC policies and **dry-run** a `(subject, action, resource)` tuple without persisting anything. |
| `audit-logs` | Search and filter the audit trail. |
| `diagnostics` | Read-only probes — login test, permission check, menu visibility — with no side effects. |
| `settings` | Live read-only view of the effective `devslab.kit.*` configuration (secrets masked). |
| `bootstrap/status` | Unauthenticated `GET` returning `{ initialized: boolean }` — the branch point for a first-run / setup wizard. |

## Authentication

All endpoints except `bootstrap/status` require a bearer token. Obtain one from
`auth/login`:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"…"}'
# → { "token": "<jwt>", "mustChangePassword": false, ... }
```

Send it as `Authorization: Bearer <jwt>` on subsequent calls. The token carries the
tenant, roles, and the `mustChangePassword` flag (see [Access](../guides/access.md)).

!!! tip "Securing the API"
    The endpoints are guarded by Spring Security and the kit's `PermissionChecker`
    (the `admin.*` permissions). Because every bean is `@ConditionalOnMissingBean`,
    you can supply your own security chain or `PermissionChecker` to change how the
    surface is protected.
