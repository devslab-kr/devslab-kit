# devslab-kit

[한국어 README](README.ko.md)

A reusable platform Spring Boot Starter for DevsLab products.

`devslab-kit` provides cross-product platform building blocks — authentication, accounts,
permissions, roles, multi/single tenancy, menus, audit logs, an admin API, and Spring Boot
AutoConfiguration — so that each product can focus on its own domain.

The kit deliberately does **not** depend on any specific product domain. Its first real consumer
is the `bookrecord` service, but `devslab-kit` itself must remain product-agnostic.

## Status

> **Pre-`0.1.0` scaffold.** The repository currently holds the initial Spring Boot 4 project
> generated from IntelliJ's New Project wizard, plus org-wide conventions (license, changelog,
> bilingual README). Multi-module split, public contracts, AutoConfiguration, and the sample app
> are tracked in [`CHANGELOG.md`](CHANGELOG.md) and land in subsequent PRs.

## Tech stack

| Layer            | Choice                                                     |
| ---------------- | ---------------------------------------------------------- |
| Language         | Java 25                                                    |
| Framework        | Spring Boot 4.x                                            |
| Build            | Gradle (Kotlin DSL)                                        |
| Group / Package  | `kr.devslab` / `kr.devslab.kit`                            |
| Web stack        | Spring Web MVC (Servlet) — WebFlux is **not** in core      |
| Security         | Spring Security (Servlet)                                  |
| Persistence      | Spring Data JPA + Flyway + PostgreSQL                      |
| Cache / session  | Spring Data Redis (Spring Session is **not** in core yet)  |
| Observability    | Spring Boot Actuator                                       |
| Native           | GraalVM Native Build Tools                                 |
| Local dev / test | Docker Compose + Testcontainers (PostgreSQL, Redis)        |

## Design principles

1. **Product-agnostic.** No `bookrecord` (or any other product) domain type may live in
   `devslab-kit`. Only platform concepts: `UserId`, `TenantId`, `Permission`, `Role`, `Menu`,
   `Audit`, etc.
2. **Java APIs are the contract.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring Session are
   **optional starters**, never core.
3. **Override-friendly AutoConfiguration.** Every default bean is `@ConditionalOnMissingBean`
   so consumer apps can replace them without forking the kit.
4. **TenantContext is always present** — even single-tenant deployments resolve a default tenant
   rather than skipping the abstraction.
5. **Permission knows nothing about Menu.** Menu may reference permissions; the dependency does
   not reverse.
6. **Auth account ≠ service profile.** `platform_user_account` holds login / status / tenancy
   only. Per-product profile data (nickname, avatar, preferences) lives in product tables.
7. **GraalVM Native friendly.** Reflection-heavy patterns are avoided; the sample app verifies
   `nativeCompile` end-to-end.

## Planned module layout

```text
devslab-kit/
├─ devslab-kit-bom
├─ devslab-kit-core
├─ devslab-kit-identity-{api,core}
├─ devslab-kit-access-{api,core}
├─ devslab-kit-tenant-{api,core}
├─ devslab-kit-menu-{api,core}
├─ devslab-kit-audit-{api,core}
├─ devslab-kit-autoconfigure
├─ devslab-kit-spring-boot-starter
├─ devslab-kit-admin-{api,ui,starter}
├─ devslab-kit-test-support
└─ devslab-kit-sample-app
```

Optional adapter starters (added on demand, not part of the default starter):

```text
devslab-kit-graphql-dgs-starter
devslab-kit-webflux-starter
devslab-kit-rabbitmq-starter
devslab-kit-spring-session-starter
devslab-kit-oauth2-{client,resource-server}-starter
```

## Running locally

```bash
./gradlew test
./gradlew bootRun
```

Docker Compose (`compose.yaml`) brings up PostgreSQL and Redis automatically via Spring Boot's
`spring-boot-docker-compose` integration during `bootRun`. Tests use Testcontainers via
`@ServiceConnection`.

## License

[Apache License 2.0](LICENSE)
