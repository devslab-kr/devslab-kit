# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The library major aligns with the Spring Boot major: `4.x.y` targets Spring Boot 4.x.

## [Unreleased]

### Added
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
- This is still the pre-`0.1.0` scaffold; the public surface may shift before `0.1.0`.
- Targeting Spring Boot 4.0.6 (release) and Java 21 instead of SB 4.1-SNAPSHOT / Java 25.
  The combination of SB SNAPSHOT + Java 25 was tripping up IntelliJ Gradle integration on
  some setups; pinning to released versions keeps the import path predictable. Will revisit
  Java 25 once SB 4.1.x ships a release.
