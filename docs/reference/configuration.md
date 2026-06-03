# Configuration

All settings live under the **`devslab.kit.*`** prefix. The live, effective values
are viewable at runtime via `GET /admin/api/v1/settings` (secrets masked).

!!! info "Value formats"
    - **Durations** use ISO-8601: `PT8H` = 8 hours, `PT15M` = 15 minutes,
      `PT10M` = 10 minutes, `PT30S` = 30 seconds, `P1D` = 1 day.
    - **Booleans** are `true` / `false`.
    - Enum-like string options list their allowed values below; anything else is
      rejected at startup.

## Tenant — `devslab.kit.tenant.*` { #tenant }

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Turn the tenant layer on/off. |
| `mode` | enum | `single` | See **mode** below. |
| `default-tenant-id` | string | `default` | The tenant id used in `single` mode, and the fallback when a resolver finds none. |
| `resolver` | enum | `fixed` | How the active tenant is determined — see **resolver** below. |
| `header` | string | `X-Tenant-Id` | Request header read by the `header` resolver. |

**`mode`**

- `single` — one tenant for the whole app (always `default-tenant-id`). The
  tenant abstraction is still present, so your code is identical to multi-tenant.
- `multi` — the tenant is determined per request by the `resolver`.

**`resolver`**

- `fixed` — always returns `default-tenant-id`. The natural pick for `single` mode.
- `header` — reads the tenant id from a request header (the `header` property,
  default `X-Tenant-Id`).
- `subdomain` — derives it from the request host's subdomain (`acme.example.com`
  → `acme`).
- `jwt` — _reserved, not yet shipped_: selecting it fails fast at startup (it awaits
  the `devslab-kit-oauth2-resource-server-starter`). The login JWT already carries a
  `tenant` claim, so resolve it from a custom `TenantResolver` bean meanwhile.

See the [Multi-tenancy guide](../guides/tenancy.md).

## Identity — `devslab.kit.identity.*` { #identity }

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `jwt.secret` | string | — | HMAC-SHA256 signing key. **Must be ≥ 32 bytes** and set in production. No default — supply your own. |
| `jwt.issuer` | string | `devslab-kit` | Value of the JWT `iss` claim; tokens are rejected on parse if it doesn't match. |
| `jwt.ttl` | duration | `PT8H` | How long an issued access token stays valid. |
| `max-failed-attempts` | int | `5` | Consecutive failed logins before the account is locked. |
| `lockout-duration` | duration | `PT15M` | How long an account stays locked after hitting the threshold. |

See the [Access guide](../guides/access.md).

## Audit — `devslab.kit.audit.*` { #audit }

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Turn audit logging on/off. |
| `async-queue-capacity` | int | `1024` | Capacity of the bounded queue feeding the async writer. When full, new audit events are dropped rather than blocking the request or exhausting memory — size it for your peak write rate. |

See the [Audit guide](../guides/audit.md).

## Menu — `devslab.kit.menu.*`

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Turn the dynamic-menu module on/off. |

See the [Menus guide](../guides/menus.md).

## Cache — `devslab.kit.cache.*` { #cache }

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `type` | enum | `in-memory` | Cache backend — see **type** below. |
| `ttl` | duration | `PT10M` | Default entry time-to-live. **Honored by `redis` only**; `in-memory` ignores it (entries live until evicted or restart). |
| `key-prefix` | string | `devslab:` | Namespace prepended to Redis keys so several apps can share one Redis without colliding. Ignored by `in-memory`. |
| `cache-null-values` | boolean | `false` | When `false`, a method that returns `null` is **not** cached (the next call re-runs it). Set `true` to cache nulls (useful against repeated misses). |
| `allowed-package` | string | `kr.devslab` | Package whose types the Redis JSON serializer trusts for polymorphic typing, in addition to `java.*`. Narrow it to your own base package for defense-in-depth. |

**`type`**

- `in-memory` — a `ConcurrentMapCacheManager`. Single-node; no TTL. The default,
  ideal for local dev and single-instance apps.
- `redis` — Spring Data Redis with the kit's JSON serializer. Entries are shared
  and consistent across replicas; honors `ttl` and `key-prefix`. Requires
  `spring.data.redis.*`.
- `none` — a `NoOpCacheManager`; caching is disabled and every lookup recomputes.

See the [Caching guide](../guides/cache.md) and [ADR 0002](../adr/0002-distributed-cache.md).

## First-admin bootstrap — `devslab.kit.bootstrap.*` { #bootstrap }

Provisions the first admin on a fresh database (ADR 0001). **Off by default**;
opt in explicitly.

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `false` | Run the bootstrap on startup. |
| `tenant-id` | string | `default` | Tenant the admin (and role/permissions) are created in. |
| `admin-login-id` | string | `admin` | Login id of the seeded admin. |
| `admin-password` | string | — | The admin's password. **Leave blank** to have a strong random one generated and logged **once** at startup. |
| `admin-email` | string | — | Optional email for the seeded admin. |
| `must-change-password` | boolean | `true` | Force the admin to set a new password on first login. |
| `seed.permissions` | list | `[]` | Permission codes to create on boot (idempotent). |
| `seed.roles` | map | `{}` | Role code → permission codes; the role is created and the listed permissions granted. |

The seed is **idempotent and additive** — on every boot it creates missing
permissions/roles and adds the listed grants, but never revokes or deletes (and a
permission a role references is auto-created). Ship starter roles so consumers don't
hand-create them:

```yaml
devslab:
  kit:
    bootstrap:
      enabled: true
      seed:
        permissions: [tasks.read, tasks.write, tasks.update, tasks.delete]
        roles:
          viewer: [tasks.read]
          editor: [tasks.read, tasks.write, tasks.update]
          owner:  [tasks.read, tasks.write, tasks.update, tasks.delete]
```

!!! warning "Production"
    Always set a strong `identity.jwt.secret`. For the bootstrap, either set a
    strong `admin-password` with `must-change-password: true`, or leave it blank
    (random, logged once), or disable the bootstrap and provision the first admin
    out-of-band. Under a `prod` / `production` profile the kit **refuses a weak
    bootstrap password**.

See the [First-admin Bootstrap guide](../guides/bootstrap.md) and
[ADR 0001](../adr/0001-bootstrap-admin.md).

## OpenAPI / Swagger UI — `devslab.kit.openapi.*` { #openapi }

The starter **bundles springdoc**, so `/swagger-ui.html` and `/v3/api-docs` come up
from the starter alone — no extra dependency, no wiring — and the kit's
`/admin/api/v1/**` endpoints are collected into one group (`/v3/api-docs/admin`).
springdoc `3.0.x` is the Spring Boot 4 line (`2.8.x` targets Spring Boot 3).

Two ways to turn it off:

1. **Keep the jar, disable the surface** — `devslab.kit.openapi.enabled=false`. The
   auto-configuration stays dormant; nothing is served. This is the usual production
   choice.
2. **Drop the jar entirely** — exclude springdoc from the starter dependency:

    === "Gradle (Kotlin DSL)"

        ```kotlin
        implementation("kr.devslab:devslab-kit-spring-boot-starter:0.5.0") {
            exclude(group = "org.springdoc")
        }
        ```

    === "Maven"

        ```xml
        <dependency>
          <groupId>kr.devslab</groupId>
          <artifactId>devslab-kit-spring-boot-starter</artifactId>
          <version>0.5.0</version>
          <exclusions>
            <exclusion>
              <groupId>org.springdoc</groupId>
              <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            </exclusion>
          </exclusions>
        </dependency>
        ```

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Master switch. Set `false` to disable the kit's OpenAPI wiring while leaving the (bundled) dependency in place. |
| `admin-group` | string | `admin` | Swagger UI group name for the admin API. |
| `title` | string | `devslab-kit Admin API` | Title shown in the OpenAPI document / Swagger UI. |
| `version` | string | `v1` | Version string in the OpenAPI document. |

Both the `OpenAPI` document bean and the admin `GroupedOpenApi` are
`@ConditionalOnMissingBean`, so you can declare your own (e.g. to add security
schemes or servers) and the kit backs off.

!!! tip "Production"
    API docs are usually not exposed in production. Set
    `devslab.kit.openapi.enabled=false` to turn the surface off (option 1 above), or
    `exclude` springdoc to drop the jar (option 2).
