# devslab-kit

[한국어 README](README.ko.md) · [Changelog](CHANGELOG.md)

A reusable **Spring Boot 4 platform starter**. Drop it into an application and get
authentication, authorization, multi-tenancy, dynamic menus and audit logging
from auto-configuration — plus an admin REST API and a ready-made admin console —
so each product can focus on its own domain.

`devslab-kit` is deliberately **product-agnostic**: it knows only platform
concepts (`UserId`, `TenantId`, `Permission`, `Role`, `Menu`, `Audit`), never any
specific product's domain.

> **Status — pre-1.0.** The platform is feature-complete for `0.1.0`, the first
> public release. Artifacts publish to Maven Central from `0.1.0` onward.

## Highlights

- **Identity** — users, credentials, JWT issue/parse, configurable login lockout.
- **Access** — roles, permissions, subject **groups**, and an **ABAC** policy SPI
  (`PolicyEvaluator`) layered over RBAC.
- **Multi-tenancy** — a tenant context that is *always present* (single-tenant
  resolves a default rather than skipping the abstraction), with pluggable
  resolvers: `fixed`, `header`, `jwt`, `subdomain`.
- **Menus** — permission-filtered dynamic menu trees, per user.
- **Audit** — async audit logging via `ApplicationEventPublisher`.
- **Cache** — a pluggable cache (`devslab.kit.cache.type` = `in-memory` / `redis`
  / `none`); the Redis backend owns JSON serialization, so you never touch
  `Serializable` or serializer wiring (ADR 0002).
- **First-admin bootstrap** — provisions a tenant, a `PLATFORM_ADMIN` role, the
  `admin.*` permissions and an admin user on first boot, with optional forced
  password change (ADR 0001).
- **Admin REST API** — `/admin/api/v1/**` for users, roles, permissions, groups,
  menus, tenants, policies, audit logs, diagnostics and settings.
- **Override-friendly** — every default bean is `@ConditionalOnMissingBean`, so
  any part is replaceable without forking the kit.

## Companion admin console

[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui) is
a Vue 3 + PrimeVue console built directly on the admin REST API — login, users,
roles, permissions, groups, menus, tenants, ABAC policy testing, audit-log
search, diagnostics and a live settings view. Use it as-is or as a reference for
your own UI.

## Install

> Available on Maven Central from `0.1.0`.

The starter pulls in the whole platform:

**Gradle (Kotlin DSL)**

```kotlin
implementation("kr.devslab:devslab-kit-spring-boot-starter:0.1.0")
```

**Maven**

```xml
<dependency>
  <groupId>kr.devslab</groupId>
  <artifactId>devslab-kit-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

You can also depend on individual modules (e.g. `devslab-kit-access-core`) if you
do not want the full starter.

### Requirements

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| Datastore | PostgreSQL (primary) |
| Cache | Redis (optional — only for the distributed cache) |

## Quick start

1. Add the starter (above).
2. Point it at a database and (optionally) Redis:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/app
       username: app
       password: app
     data:
       redis:
         host: localhost      # only needed when cache.type = redis

   devslab:
     kit:
       tenant:
         mode: single         # or multi
         resolver: fixed      # fixed | header | jwt | subdomain
       cache:
         type: in-memory      # in-memory | redis | none
       bootstrap:
         enabled: true        # provision the first admin on first boot
   ```

3. Boot the app. The first-admin bootstrap seeds a `PLATFORM_ADMIN`, and the
   admin REST API is live at `/admin/api/v1/**`. Point
   [devslab-kit-admin-ui](https://github.com/devslab-kr/devslab-kit-admin-ui) at
   it to log in.

See [`devslab-kit-sample-app`](devslab-kit-sample-app) for a complete, runnable
configuration (Docker Compose for Postgres + Redis, Testcontainers for tests).

## Modules

| Module | Purpose |
| --- | --- |
| `devslab-kit-core` | Shared value objects (`TenantId`, `UserId`, `PublicId`, …) |
| `devslab-kit-tenant-{api,core}` | Tenant context + resolvers (fixed/header/jwt/subdomain) |
| `devslab-kit-identity-{api,core}` | Users, credentials, JWT, login lockout |
| `devslab-kit-access-{api,core}` | Roles, permissions, groups, ABAC policy engine |
| `devslab-kit-menu-{api,core}` | Permission-filtered dynamic menus |
| `devslab-kit-audit-{api,core}` | Async audit logging |
| `devslab-kit-cache-{api,core}` | Pluggable cache (in-memory / Redis) |
| `devslab-kit-admin-api` | Admin REST endpoints (`/admin/api/v1/**`) |
| `devslab-kit-autoconfigure` | Spring Boot auto-configuration |
| `devslab-kit-spring-boot-starter` | The starter — pulls in the whole platform |
| `devslab-kit-sample-app` | Runnable reference app (not published) |

`*-api` modules hold the public contracts; `*-core` modules hold the default
implementations. Depend on an `-api` alone to implement your own.

## Design principles

1. **Product-agnostic.** Only platform concepts live here — never a product's
   domain types.
2. **Java APIs are the contract.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring
   Session are optional add-ons, never core.
3. **Override-friendly auto-configuration.** Every default bean is
   `@ConditionalOnMissingBean`.
4. **TenantContext is always present** — even single-tenant resolves a default.
5. **Permission knows nothing about Menu.** Menus may reference permissions; the
   dependency never reverses.
6. **Auth account ≠ service profile.** The platform account holds login / status /
   tenancy only; per-product profile data lives in product tables.
7. **GraalVM Native friendly.** Reflection-heavy patterns are avoided.

## Documentation

- Architecture decisions: [`docs/adr`](docs/adr) — ADR 0001 (first-admin
  bootstrap), ADR 0002 (pluggable cache). Both bilingual (en + ko).
- Changelog: [`CHANGELOG.md`](CHANGELOG.md) ([한국어](CHANGELOG.ko.md)).

## Running locally

```bash
./gradlew build              # compile + test (Testcontainers: Postgres + Redis)
./gradlew :devslab-kit-sample-app:bootRun
```

## License

[Apache License 2.0](LICENSE)
