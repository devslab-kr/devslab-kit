# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The library major aligns with the Spring Boot major: `4.x.y` targets Spring Boot 4.x.

한국어: [CHANGELOG.ko.md](CHANGELOG.ko.md)

## [Unreleased]

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
