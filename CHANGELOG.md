# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The library major aligns with the Spring Boot major: `4.x.y` targets Spring Boot 4.x.

## [Unreleased]

### Added
- Initial project scaffold (Spring Boot 4 + Java 25 + Gradle).
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
- Identity contracts (api-only): `CurrentUser`, `CurrentUserProvider`, `UserStatus`.
- Access contracts (api-only): `Permission`, `Role`, `PermissionChecker`,
  `PermissionDeniedException`.
- Menu contracts (api-only): `MenuItem`, `MenuTree`, `MenuProvider`.
- Audit contracts (api-only): `AuditEvent`, `AuditActor`, `AuditAction`, `AuditTarget`,
  `AuditEventPublisher`.
- `DevslabKitProperties` (`devslab.kit.*` prefix) + `TenantAutoConfiguration` with
  `@ConditionalOnMissingBean` overrides.
- `devslab-kit-sample-app` verifies the starter loads and resolves the configured tenant.

### Notes
- `-identity-core`, `-access-core`, `-menu-core`, `-audit-core` are module skeletons only
  (no source files yet). Concrete implementations land in subsequent PRs.
- This is still the pre-`0.1.0` scaffold; the public surface may shift before `0.1.0`.
