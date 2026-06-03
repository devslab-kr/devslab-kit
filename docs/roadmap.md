# Roadmap

A rough, non-binding view of where `devslab-kit` is headed. Priorities shift with
real usage — [open an issue](https://github.com/devslab-kr/devslab-kit/issues) to
weigh in.

## Where it is today

`devslab-kit` is published on Maven Central — latest **0.5.0** — alongside this
documentation site and the companion
[admin console](https://github.com/devslab-kr/devslab-kit-admin-ui). The platform is
feature-complete for its scope:

- Identity, Access (RBAC + groups + ABAC), multi-tenancy, dynamic menus, audit,
  pluggable cache, first-admin bootstrap, admin REST API.

Shipped since the first release (`0.1.0`):

- **Config sync across environments** (`0.4.0`) — export/import definitional config
  (permissions, roles, menus; opt-in users) as a code-keyed bundle, `merge`/`mirror`,
  dry-run by default ([guide](guides/config-sync.md) · [ADR 0003](adr/0003-config-sync.md)).
- **Flyway history-table separation + RFC 7807 ProblemDetail** (`0.3.0`).
- **OpenAPI / Swagger UI for the admin API** (`0.2.0`).

## Candidates (not committed)

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
