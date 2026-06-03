# First-admin Bootstrap

A fresh database has no users — so how do you log in the first time without a
permanent backdoor? The kit's **first-admin bootstrap** provisions a usable admin
on first boot: opt-in, property-driven, and idempotent (background:
[ADR 0001](../adr/0001-bootstrap-admin.md)).

New here? This is exactly what the [Tutorial](../getting-started/tutorial.md) uses to
get you logged in — this guide is the reference behind that step.

## What it does

When `bootstrap.enabled = true`, on startup the kit **idempotently** creates (if
absent):

1. the tenant `bootstrap.tenant-id`,
2. a `PLATFORM_ADMIN` role with the full `admin.*` permission set,
3. an admin user (`bootstrap.admin-login-id`) in that tenant, holding that role.

Idempotent means it's safe to leave on — subsequent boots find the records and do
nothing.

## Configuration

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    bootstrap:
      enabled: true
      tenant-id: default
      admin-login-id: admin
      admin-password: ${DEVSLAB_BOOTSTRAP_ADMIN_PASSWORD:}   # blank → random, logged once
      must-change-password: true
```

See the [Configuration reference](../reference/configuration.md#bootstrap) for every key.

## The password

- **Set explicitly** for a known value (e.g. local dev `admin`/`admin`).
- **Leave blank** and the kit generates a strong random password and logs it
  **once** at startup — copy it from the logs, then it's gone.
- Under a `prod` / `production` profile the kit **refuses to start** with a weak
  bootstrap password, so a placeholder can't leak into production.

Pair a known password with `must-change-password: true` so the operator rotates
it on first login.

## Log in for the first time

With the config above (`admin`/`admin`), start the app, then:

=== "Admin console"

    1. Start the [admin console](admin-console.md) and open it in the browser.
    2. Log in with `admin` / `admin` (tenant `default`).
    3. You now hold `PLATFORM_ADMIN` — every screen is available.

=== "REST API"

    ```bash
    # exchange credentials for a JWT:
    curl -X POST localhost:8080/admin/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","loginId":"admin","rawPassword":"admin"}'
    # → { "token": "eyJ…" }  — send it as `Authorization: Bearer eyJ…`
    ```

## First-run detection

The unauthenticated endpoint `GET /admin/api/v1/bootstrap/status` returns
`{ "initialized": boolean }`. A setup wizard or landing page can branch on it —
e.g. send a brand-new deployment to a "create your admin" flow instead of a login
form:

```bash
curl localhost:8080/admin/api/v1/bootstrap/status
# → { "initialized": false }   (no admin yet)  /  true (provisioned)
```

It's public on purpose — the wizard calls it *before* anyone can authenticate.

## Production guidance

For real environments, prefer one of:

- a strong `admin-password` (injected as a secret) + `must-change-password: true`, or
- `enabled: false` and provision the first admin out-of-band (SQL/migration/ops tooling).

## See also

- [Tutorial](../getting-started/tutorial.md) — bootstrap in a running app.
- [Access (RBAC + ABAC)](access.md) — what `PLATFORM_ADMIN` and `admin.*` grant.
- [Configuration reference](../reference/configuration.md#bootstrap) — every key.
