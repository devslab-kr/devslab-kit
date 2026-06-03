# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The library major aligns with the Spring Boot major: `4.x.y` targets Spring Boot 4.x.

한국어: [CHANGELOG.ko.md](CHANGELOG.ko.md)

## [Unreleased]

## [0.4.0] — 2026-06-03

### Added
- **Config sync across environments (ADR 0003).** Promote definitional platform config —
  permissions, roles (+ their permission codes) and menus — from one environment to another as
  a portable, code-keyed bundle, instead of hand-editing each target database.
  - `GET /admin/api/v1/config/export` and `POST /admin/api/v1/config/import`.
  - **merge** (default, additive — creates and updates, never deletes) and **mirror** (makes the
    target match the bundle: reconciles each role's grants and deletes definitional entities
    absent from the bundle — menus leaf-first; a role still assigned to a user is skipped;
    permissions revoked then deleted).
  - **dry-run by default** — the import returns a per-section diff (created / updated / deleted /
    skipped) and writes nothing unless `dryRun=false`.
  - **Opt-in user sync** (`includeUsers`, default off): export carries users by login id with
    **no password**; import is create-only and never overwrites an existing user.
  - **Off by default** (`devslab.kit.config-sync.enabled`) and **refused under a production
    profile** (`prod`/`production`, ADR 0003 §5) — promote config via the committed bundle on
    deploy, not an ad-hoc push to prod.
  - A **Config Sync** page in the [admin console](https://github.com/devslab-kr/devslab-kit-admin-ui)
    drives export / import / dry-run diff / apply.

### Notes
- No migration required: config sync adds no tables and is inert unless explicitly enabled.

## [0.3.0] — 2026-06-02

### Changed
- **The kit's Flyway migrations now run on their own schema-history table.** They
  moved from the default `classpath:db/migration` (`flyway_schema_history`) to
  `classpath:db/devslab-kit` (`devslab_kit_schema_history`), run by a dedicated
  programmatic Flyway in `KitFlywayAutoConfiguration` (not a `Flyway` bean, so the
  consumer's auto-configured Flyway is left intact). The consumer's default
  `classpath:db/migration` + `flyway_schema_history` is left entirely to the app, so
  a consumer can ship its own `V1__*.sql` with **no version collision**, and the kit
  can add migrations across releases without disturbing the consumer's ordering. The
  consumer's Flyway runs first (on an empty schema); the kit runs second with
  `baselineOnMigrate`. **No consumer configuration is required.**
  - **Upgrade:** a `0.2.x` database that already applied the kit migrations on the
    default `flyway_schema_history` must be recreated (pre-1.0; only demo/local DBs
    exist). New databases need no action.

### Changed (breaking)
- **The admin API now returns RFC 7807 `ProblemDetail`** (`application/problem+json`)
  for every error, replacing the bespoke `ApiError` body. The members are
  `type` / `title` / `status` / `detail`, plus an `errors` extension carrying
  per-field validation messages. Read the human-readable message from `detail`
  (falling back to `title`) — the previous top-level `message` field is gone, and the
  `kr.devslab.kit.admin.ApiError` record was removed. `devslab-kit-admin-ui` is
  updated in lockstep to read `detail`.

## [0.2.1] — 2026-06-02

### Changed
- **OpenAPI / Swagger UI is now bundled in the starter** — `0.2.0` shipped springdoc
  as `compileOnly`, so Swagger UI only appeared if the consumer added springdoc
  themselves. The starter now depends on `springdoc-openapi-starter-webmvc-ui`
  (`api`), so `/swagger-ui` and `/v3/api-docs` come up from the starter alone —
  matching the kit's "add the starter, nothing else" promise. Turn it off with
  `devslab.kit.openapi.enabled=false`, or `exclude` the `org.springdoc` dependency to
  drop the jar (both documented in the configuration reference).

## [0.2.0] — 2026-06-02

### Added
- **OpenAPI / Swagger UI auto-configuration** — when springdoc-openapi is on the
  classpath (declared `compileOnly` by the kit; a consumer opts in by adding
  `springdoc-openapi-starter-webmvc-ui`), `/swagger-ui.html` and `/v3/api-docs`
  come up with no wiring and the `/admin/api/v1/**` endpoints are collected into one
  group. Disable without removing the dependency via `devslab.kit.openapi.enabled=false`
  (typical in production). The `OpenAPI` document and admin `GroupedOpenApi` beans are
  `@ConditionalOnMissingBean`, so a consumer can override either. Targets springdoc
  `3.0.x` (the Spring Boot 4 line).

## [0.1.0] — 2026-06-01

First public release.

### Added
- **Maven Central publishing** — every library module ships to Maven Central via
  the vanniktech maven-publish plugin (Central Portal, signed, auto-release on a
  `v*` tag). `release.yml` publishes the artifacts and opens a GitHub Release.
- **Zero-config JPA self-registration** — the starter contributes its own
  `@Entity` types and Spring Data repositories to a consumer running a plain
  `@SpringBootApplication` in any package, with no `@EntityScan`,
  `@EnableJpaRepositories`, or `scanBasePackages`. It broadens scanning rather
  than replacing it, so the consumer's own entities and repositories keep working
  (proven by an external-consumer integration test under `com.example.consumer`).
- **Zero-config admin API web layer** — the admin controllers, error handler, and
  security chain auto-register too, so `/admin/api/v1/**` comes up from the starter
  alone (servlet web apps), again with no component-scan configuration.
- **Authorization enforced on the admin API** — every `/admin/api/v1/**` endpoint
  requires the `admin.*` permission it maps to (read → `*.read`, mutating →
  `*.write`), enforced by the kit's security chain against the caller's effective
  permissions (resolved per request from their roles and groups, so a grant or
  revocation takes effect on the next call). The first-admin bootstrap seeds every
  `admin.*` permission onto `PLATFORM_ADMIN`, so the seeded admin can use the whole
  API immediately. `login` and `bootstrap/status` stay public.
- **Pluggable cache** (ADR 0002) — `devslab.kit.cache.type` = `in-memory` /
  `redis` / `none`. The Redis backend owns JSON serialization (no `Serializable`,
  no serializer wiring), and the per-user menu cache now rides this shared cache
  manager instead of its own map.
- **First-admin bootstrap** (ADR 0001) — opt-in, property-driven provisioning so
  a fresh database can reach a usable dashboard without a permanent backdoor.
  `devslab.kit.bootstrap.*` (OFF by default) idempotently creates a tenant, a
  `PLATFORM_ADMIN` role with the full `admin.*` permission set, and one admin
  user on first boot. A blank password generates a strong random one logged
  once; a prod safety pin refuses a weak password under a `prod`/`production`
  profile.
- **Forced password change** — `must_change_password` flag (`V11`) on the user
  account, surfaced through `CurrentUser`, the JWT claim, and the login
  response. Self-service `POST /admin/api/v1/auth/change-password` verifies the
  old password, sets the new one, clears the flag, and re-issues a token.
- **Bootstrap status probe** — unauthenticated `GET /admin/api/v1/bootstrap/status`
  returning `{ initialized: boolean }`, the branch point for a future guided
  first-run / setup wizard (ADR 0001 §6).

### Fixed
- **JWT validation now honours the injected `Clock`** — `JjwtAuthTokenService.parse()`
  validated token expiry against the real system clock instead of the injected one,
  making validation untestable with a fixed clock and asymmetric with `issue()`.
  Production behaviour is unchanged (the runtime uses `Clock.systemUTC()` on both
  paths).

### Changed
- `sample-app` switched off its `SampleSeedRunner` onto the starter's
  `devslab.kit.bootstrap.*` runner (local-dev shape: `admin/admin`,
  `must-change-password=false`).

### Added (initial scaffold)
- Initial project scaffold (Spring Boot 4 + Java 21 + Gradle).
- Base dependencies: Spring Web MVC, Spring Security, Spring Data JPA, Spring Data Redis,
  Flyway (PostgreSQL), Spring Boot Actuator, GraalVM Native, Testcontainers (PostgreSQL + Redis),
  Docker Compose support.
- Base package `kr.devslab.kit`.
- Gradle multi-module split into 14 modules (planning doc §5):
  `devslab-kit-core`, `-{identity,access,tenant,menu,audit}-{api,core}`,
  `-autoconfigure`, `-spring-boot-starter`, `-sample-app`.
- Core value objects: `UserId`, `TenantId`, `RoleId`, `PermissionId`, `MenuId`, `PublicId`,
  `DevslabKitException`.
- Tenant: `TenantContext`, `TenantContextHolder`, `TenantResolver`, `TenantMode` (api) +
  `DefaultTenantContextHolder`, `FixedTenantResolver` (core). The one fully wired vertical
  used to prove the AutoConfig override pattern.
- Identity (api): `CurrentUser`, `CurrentUserProvider`, `UserStatus`, `LoginCommand`,
  `LoginResult`, `UserAccountView`, `PasswordHasher`, `LoginFailureReason`,
  `AccountLoginException`, `LoginSucceededEvent`, `LoginFailedEvent`,
  `UserAccountCreatedEvent`.
  Identity (core, first-pass): `PlatformUserAccountEntity` + `JpaPlatformUserAccountRepository`,
  `BCryptPasswordHasher`, `LocalLoginService`, `PlatformUserAccountService`,
  `DefaultCurrentUserProvider`, `V1__platform_user_account.sql`.
- Access (api): `Permission`, `Role`, `PermissionChecker`, `PermissionDeniedException`.
  Access (core, first-pass): `Platform{Role,Permission,UserRole,RolePermission}Entity`
  + Jpa repos, `UserRoleService`, `RolePermissionService`, `DefaultPermissionChecker`,
  `V2__platform_access.sql`.
- Menu (api): `MenuItem`, `MenuTree`, `MenuProvider`.
  Menu (core, first-pass): `PlatformMenuEntity` + `JpaPlatformMenuRepository`,
  `MenuTreeBuilder`, `PermissionBasedMenuFilter`, `DefaultMenuProvider`,
  `V3__platform_menu.sql`.
- Audit (api): `AuditEvent`, `AuditActor`, `AuditAction`, `AuditTarget`,
  `AuditEventPublisher`.
  Audit (core, first-pass): `PlatformAuditLogEntity` + `JpaPlatformAuditLogRepository`,
  `AuditLogService` (Jackson-serialized metadata), `DefaultAuditEventPublisher`,
  `V4__platform_audit_log.sql`.
- `DevslabKitProperties` (`devslab.kit.*` prefix) + 5 `AutoConfiguration`s with
  `@ConditionalOnMissingBean` overrides: `Tenant`, `Identity`, `Access`, `Menu`, `Audit`.
- `devslab-kit-sample-app` smoke-tests all 8 starter beans (`TenantResolver`,
  `TenantContextHolder`, `CurrentUserProvider`, `PasswordHasher`, `LocalLoginService`,
  `PermissionChecker`, `MenuProvider`, `AuditEventPublisher`) plus a BCrypt round-trip.

### Notes
- Targeting Spring Boot 4.0.6 (release) and Java 21 instead of SB 4.1-SNAPSHOT / Java 25.
  The combination of SB SNAPSHOT + Java 25 was tripping up IntelliJ Gradle integration on
  some setups; pinning to released versions keeps the import path predictable. Will revisit
  Java 25 once SB 4.1.x ships a release.
