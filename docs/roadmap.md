# Roadmap

A rough, non-binding view of where `devslab-kit` is headed. Priorities shift with
real usage — [open an issue](https://github.com/devslab-kr/devslab-kit/issues) to
weigh in.

## 0.1.0 — first public release

The current focus: ship the platform that's already feature-complete.

- Identity, Access (RBAC + groups + ABAC), multi-tenancy, dynamic menus, audit,
  pluggable cache, first-admin bootstrap, admin REST API.
- Maven Central publishing + this documentation site.
- Companion [admin console](https://github.com/devslab-kr/devslab-kit-admin-ui).

## After 0.1.0 (candidates)

- **GraalVM native** — the sample app exercises `nativeCompile`; promote it to a
  verified, documented path.
- **Optional adapter starters** — kept *out* of core by design, added on demand:
  WebFlux, GraphQL, RabbitMQ, Spring Session, OAuth2 (client / resource server).
- **More tenant resolvers / cache backends** as real consumers need them
  (the "pull, don't push" rule — extract when there's a second consumer).
- **Hardening** — more integration coverage, security review, performance passes.

## Versioning

The library major tracks the Spring Boot major: **`4.x.y` targets Spring Boot
4.x**. A Spring Boot 4.1 line would arrive as its own `4.1`-aligned releases. See
the [changelog](changelog.md) for what has actually shipped.
