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

## Authorization

Authentication is not enough: every endpoint also requires the `admin.*` permission
it maps to. Read endpoints require the matching `*.read`, mutating endpoints `*.write`
— for example `GET /users` needs `admin.user.read` and `POST /users` needs
`admin.user.write`. `auth/login` and `bootstrap/status` are public; `auth/change-password`
only needs a valid token.

Enforcement happens in the kit's security chain, checked against the caller's
**effective permissions** — resolved per request from the roles and groups they hold
(the same grant resolution the [`PermissionChecker`](../guides/access.md) uses), not
read from the token. So granting or revoking a permission takes effect on the caller's
next request, and the JWT stays small. The first-admin bootstrap seeds every `admin.*`
permission onto `PLATFORM_ADMIN`, so the seeded admin can use the whole API at once.

!!! tip "Customising security"
    The security chain and the JWT filter are both `@ConditionalOnMissingBean`, so you
    can supply your own `SecurityFilterChain` or `JwtAuthenticationFilter` to change how
    the surface is protected. For authorization checks inside your own code, inject the
    kit's `PermissionChecker` (see [Access](../guides/access.md)).

## Errors

Every error response is an RFC 7807 [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc7807)
(`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": ["loginId: must not be blank"]
}
```

Read the human-readable message from `detail` (falling back to `title`). Validation
failures add an `errors` array of per-field messages. Common statuses: `401` (bad
credentials), `403` (missing the required `admin.*` permission), `400` (bad input),
`409` (conflict).
