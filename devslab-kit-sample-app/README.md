# devslab-kit-sample-app

[한국어](README.ko.md)

A small Spring Boot application that consumes `devslab-kit-spring-boot-starter`.
It is **not published to Maven Central** — it exists to serve the library, not to
be depended on.

## Why this module exists

### 1. Integration test harness (the main reason it's kept in this repo)

This is where we prove the starter actually works *as a consumer would use it* —
not just that the code compiles. It boots the **full auto-configuration** against
**real PostgreSQL and Redis** (via Testcontainers) and asserts the whole platform
wires up:

- `SampleApplicationTests` brings up the context and checks every starter bean is
  present and usable — `TenantResolver`, `TenantContextHolder`,
  `CurrentUserProvider`, `PasswordHasher`, `LocalLoginService`,
  `PermissionChecker`, `MenuProvider`, `AuditEventPublisher` — plus a BCrypt
  round-trip.
- `BootstrapStatusEndpointTests` verifies the first-admin bootstrap (ADR 0001)
  and that the admin REST API responds.
- `TestcontainersConfiguration` stands up Postgres + Redis and wires them in with
  `@ServiceConnection`.

Unit tests can't catch this class of problem — auto-configuration ordering, the
Spring Boot BOM, JPA/Flyway against a real dialect, JSONB binding, and Redis JSON
(de)serialization only fail when the whole thing boots against real
infrastructure. Several real bugs (e.g. the Redis cache serializer and the JWT
clock handling) surfaced here first. **CI depends on this module**, so it stays
in-repo.

### 2. Living reference configuration

[`src/main/resources/application.yaml`](src/main/resources/application.yaml) is a
complete, working example of how to configure the starter — datasource, Redis,
and the `devslab.kit.*` knobs (tenant mode/resolver, cache type, audit,
first-admin bootstrap). Copy it as a starting point for a real app.

### 3. Local-dev playground & GraalVM native target

Run it to click around the admin REST API (point
[devslab-kit-admin-ui](https://github.com/devslab-kr/devslab-kit-admin-ui) at it
to log in), and it's the module used to verify `nativeCompile` end-to-end.

## Running

### Tests (no setup beyond Docker)

```bash
./gradlew :devslab-kit-sample-app:test
```

Testcontainers starts throwaway Postgres + Redis automatically; you only need a
running Docker.

### bootRun (Docker only — compose is automatic)

The module ships a [`compose.yaml`](compose.yaml) (Postgres + Redis) and depends
on `spring-boot-docker-compose`, so `bootRun` starts those containers and wires
the connection automatically. You just need Docker running:

```bash
./gradlew :devslab-kit-sample-app:bootRun
```

The `DEVSLAB_*` defaults in `application.yaml` (localhost Postgres/Redis) are the
fallback for pointing at an *external* datastore instead — e.g.
`DEVSLAB_DATASOURCE_URL`, `DEVSLAB_REDIS_HOST`, `DEVSLAB_CACHE_TYPE` for CI or
staging. The bootstrap seeds `admin`/`admin` with the forced password change off
for local convenience — do **not** use that shape in production.

## Not on Maven Central — by design

This module is excluded from publishing (`nonPublishedModules` in the root
`build.gradle.kts`). It's a test/reference harness; only the library modules ship.
A user-facing demo, if we add one, would live in
[devslab-examples](https://github.com/devslab-kr) rather than here.
