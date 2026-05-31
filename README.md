# devslab-kit

[![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/devslab-kit-spring-boot-starter?logo=apachemaven)](https://central.sonatype.com/artifact/kr.devslab/devslab-kit-spring-boot-starter)
[![Build](https://github.com/devslab-kr/devslab-kit/actions/workflows/build.yml/badge.svg)](https://github.com/devslab-kr/devslab-kit/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot)

[한국어 README](README.ko.md) · [Changelog](CHANGELOG.md) · [ADRs](docs/adr)

A reusable **Spring Boot 4 platform starter**. Drop it into an application and get
authentication, authorization, multi-tenancy, dynamic menus and audit logging
from auto-configuration — plus an admin REST API and a ready-made admin console —
so each product can focus on its own domain instead of rebuilding the platform
layer every time.

`devslab-kit` is deliberately **product-agnostic**: it knows only platform
concepts (`UserId`, `TenantId`, `Permission`, `Role`, `Menu`, `Audit`), never a
specific product's domain.

> **Status — pre-1.0.** The platform is feature-complete for `0.1.0`, the first
> public release. Artifacts publish to Maven Central from `0.1.0` onward; until
> then, build from source or use `publishToMavenLocal`.

## Contents

- [Features](#features) · [Requirements](#requirements) · [Install](#install)
- [Quick start](#quick-start) · [Configuration](#configuration)
- [Modules](#modules) · [Admin REST API](#admin-rest-api) · [Admin console](#admin-console)
- [Design principles](#design-principles) · [Documentation](#documentation)
- [Building from source](#building-from-source) · [Versioning](#versioning) · [License](#license)

## Features

| Area | What you get |
| --- | --- |
| **Identity** | User accounts, BCrypt credentials, JWT issue/parse, configurable login lockout, forced password change. |
| **Access** | Roles, permissions, subject **groups**, and an **ABAC** policy SPI (`PolicyEvaluator`) layered on top of RBAC. |
| **Multi-tenancy** | A tenant context that is *always present* (single-tenant resolves a default rather than skipping the abstraction), with pluggable resolvers: `fixed`, `header`, `jwt`, `subdomain`; `single` and `multi` modes. |
| **Menus** | Permission-filtered dynamic menu trees, computed per user. |
| **Audit** | Asynchronous audit logging through `ApplicationEventPublisher`, persisted to PostgreSQL (JSONB metadata). |
| **Cache** | A pluggable cache — `in-memory`, `redis`, or `none`. The Redis backend owns JSON serialization, so you never implement `Serializable` or wire a serializer (ADR 0002). The per-user menu cache rides this shared manager. |
| **First-admin bootstrap** | Idempotently provisions a tenant, a `PLATFORM_ADMIN` role, the `admin.*` permissions, and an admin user on first boot — opt-in and property-driven (ADR 0001). |
| **Admin REST API** | `/admin/api/v1/**` for every entity above, plus diagnostics and a live settings view. |
| **Override-friendly** | Every default bean is `@ConditionalOnMissingBean` — replace any piece by declaring your own. |
| **GraalVM Native** | Reflection-heavy patterns are avoided; the sample app verifies `nativeCompile`. |

## Requirements

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| Datastore | PostgreSQL (primary; Flyway-migrated) |
| Cache | Redis (optional — only when `cache.type = redis`) |
| Web stack | Spring Web MVC (Servlet) + Spring Security |

## Install

> Available on Maven Central from `0.1.0`. The starter pulls in the whole platform.

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

Prefer à la carte? Depend on an individual module (e.g. `devslab-kit-access-core`,
or just the `devslab-kit-access-api` contract to supply your own implementation).

## Quick start

**1. Add the starter** (above).

**2. Configure** a datasource and the platform:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: app
    password: app
  data:
    redis:
      host: localhost          # only needed when cache.type = redis

devslab:
  kit:
    tenant:
      mode: single             # single | multi
      resolver: fixed          # fixed | header | jwt | subdomain
      default-tenant-id: default
    identity:
      jwt:
        secret: ${DEVSLAB_JWT_SECRET}   # 32+ bytes for HS256 — set in prod
        ttl: PT8H
      max-failed-attempts: 5            # lock the account after N failures
      lockout-duration: PT15M
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # provision the first admin on first boot
```

**3. Boot the app.** The bootstrap seeds a `PLATFORM_ADMIN`, the admin REST API
goes live at `/admin/api/v1/**`, and Flyway creates the `platform_*` tables. Point
the [admin console](#admin-console) at it to log in.

A complete, runnable configuration (with Docker Compose for Postgres + Redis and
Testcontainers-backed tests) lives in
[`devslab-kit-sample-app`](devslab-kit-sample-app) — see
[its README](devslab-kit-sample-app/README.md).

## Configuration

All keys are under the `devslab.kit.*` prefix. Defaults shown.

| Key | Default | Notes |
| --- | --- | --- |
| `tenant.enabled` | `true` | Master switch for the tenant layer. |
| `tenant.mode` | `single` | `single` or `multi`. |
| `tenant.default-tenant-id` | `default` | Tenant used in single mode / as fallback. |
| `tenant.resolver` | `fixed` | `fixed` · `header` · `jwt` · `subdomain`. |
| `identity.jwt.secret` | — | 32+ byte HS256 key. **Required in production.** |
| `identity.jwt.issuer` | `devslab-kit` | JWT `iss` claim. |
| `identity.jwt.ttl` | `PT8H` | Token lifetime (ISO-8601 duration). |
| `identity.max-failed-attempts` | `5` | Lock the account after this many failures. |
| `identity.lockout-duration` | `PT15M` | How long an account stays locked. |
| `audit.enabled` | `true` | Toggle audit logging. |
| `audit.async-queue-capacity` | `1024` | Bounded queue for the async publisher. |
| `menu.enabled` | `true` | Toggle dynamic menus. |
| `cache.type` | `in-memory` | `in-memory` · `redis` · `none`. |
| `cache.ttl` | `PT10M` | Entry TTL (used by the Redis backend). |
| `cache.key-prefix` | `devslab:` | Redis key namespace. |
| `cache.allowed-package` | `kr.devslab` | Allow-list for safe polymorphic JSON typing. |
| `bootstrap.enabled` | `false` | Provision the first admin on first boot. |
| `bootstrap.admin-login-id` | `admin` | First admin login id. |
| `bootstrap.admin-password` | — | Blank → a strong random one is logged once. |
| `bootstrap.must-change-password` | `true` | Force a rotation on first login. |

The live, effective values are also viewable at runtime via
`GET /admin/api/v1/settings` (secrets masked).

## Modules

| Module | Purpose |
| --- | --- |
| `devslab-kit-core` | Shared value objects (`TenantId`, `UserId`, `PublicId`, …) |
| `devslab-kit-tenant-{api,core}` | Tenant context + resolvers |
| `devslab-kit-identity-{api,core}` | Users, credentials, JWT, login lockout |
| `devslab-kit-access-{api,core}` | Roles, permissions, groups, ABAC policy engine |
| `devslab-kit-menu-{api,core}` | Permission-filtered dynamic menus |
| `devslab-kit-audit-{api,core}` | Async audit logging |
| `devslab-kit-cache-{api,core}` | Pluggable cache (in-memory / Redis) |
| `devslab-kit-admin-api` | Admin REST endpoints |
| `devslab-kit-autoconfigure` | Spring Boot auto-configuration |
| `devslab-kit-spring-boot-starter` | The starter — pulls in the whole platform |
| `devslab-kit-sample-app` | Runnable reference app + integration harness (not published) |

**`-api` vs `-core`.** Each capability is split into a thin contract module
(`-api`) and a default implementation (`-core`). Depend on a `-core` for the
batteries-included default, or on an `-api` alone to plug in your own — the
auto-configuration backs off (`@ConditionalOnMissingBean`) when you do.

## Admin REST API

All under `/admin/api/v1`:

| Resource | Endpoints |
| --- | --- |
| `auth` | login, change password |
| `users` · `roles` · `permissions` · `groups` | full CRUD + assignments |
| `menus` · `tenants` | manage menu trees and tenants |
| `policies` | list ABAC policies + dry-run a `(subject, action, resource)` tuple |
| `audit-logs` | search/filter the audit trail |
| `diagnostics` | read-only login / permission / menu-visibility probes |
| `settings` | live `devslab.kit.*` view (secrets masked) |
| `bootstrap/status` | unauthenticated `{ initialized: boolean }` for first-run flows |

## Admin console

[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui) is
a Vue 3 + PrimeVue console built directly on this REST API — login, the full set
of entity screens, ABAC policy testing, audit-log search, diagnostics and a live
settings view, all bilingual (en/ko). Use it as-is or as a reference for your own.

## Design principles

1. **Product-agnostic.** Only platform concepts live here — never a product's domain types.
2. **Java APIs are the contract.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring Session are optional add-ons, never core.
3. **Override-friendly auto-configuration.** Every default bean is `@ConditionalOnMissingBean`.
4. **TenantContext is always present** — even single-tenant resolves a default.
5. **Permission knows nothing about Menu.** Menus may reference permissions; the dependency never reverses.
6. **Auth account ≠ service profile.** The platform account holds login / status / tenancy only; per-product profile data lives in product tables.
7. **GraalVM Native friendly.** Reflection-heavy patterns are avoided.

## Documentation

- **Architecture decisions** — [`docs/adr`](docs/adr): ADR 0001 (first-admin
  bootstrap), ADR 0002 (pluggable cache). Bilingual (en + ko).
- **Changelog** — [`CHANGELOG.md`](CHANGELOG.md) ([한국어](CHANGELOG.ko.md)).

## Building from source

```bash
./gradlew build                              # compile + test (Testcontainers: Postgres + Redis; needs Docker)
./gradlew publishToMavenLocal                # install all modules to ~/.m2
./gradlew :devslab-kit-sample-app:bootRun    # run the reference app
```

Java 21 (the build uses a GraalVM 21 toolchain) and a running Docker for the
integration tests.

## Versioning

The library major aligns with the Spring Boot major: **`4.x.y` targets Spring
Boot 4.x**. Releases follow [Semantic Versioning](https://semver.org/); see the
[changelog](CHANGELOG.md) for migration notes.

## License

[Apache License 2.0](LICENSE)
