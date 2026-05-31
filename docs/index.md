# devslab-kit

A reusable **Spring Boot 4 platform starter**. Drop it into an application and get
authentication, authorization, multi-tenancy, dynamic menus and audit logging from
auto-configuration — plus an admin REST API and a ready-made admin console — so each
product can focus on its own domain instead of rebuilding the platform layer.

`devslab-kit` is deliberately **product-agnostic**: it knows only platform concepts
(`UserId`, `TenantId`, `Permission`, `Role`, `Menu`, `Audit`), never a specific
product's domain.

[Get started](getting-started/installation.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/devslab-kr/devslab-kit){ .md-button }

!!! note "Status — pre-1.0"
    The platform is feature-complete for `0.1.0`, the first public release.
    Artifacts publish to Maven Central from `0.1.0` onward.

## What you get

<div class="grid cards" markdown>

-   :material-account-key: **Identity**

    Users, BCrypt credentials, JWT issue/parse, configurable login lockout, forced
    password change.

-   :material-shield-lock: **Access**

    Roles, permissions, subject **groups**, and an **ABAC** policy SPI on top of RBAC.

-   :material-domain: **Multi-tenancy**

    A tenant context that is *always present*, with pluggable resolvers — `fixed`,
    `header`, `jwt`, `subdomain`.

-   :material-menu: **Dynamic menus**

    Permission-filtered menu trees, computed per user.

-   :material-clipboard-text-clock: **Audit logging**

    Asynchronous audit trail through `ApplicationEventPublisher`, persisted to
    PostgreSQL.

-   :material-database-arrow-right: **Pluggable cache**

    `in-memory`, `redis`, or `none`. The Redis backend owns JSON serialization — no
    `Serializable`, no serializer wiring.

</div>

## Why a starter?

Every product the team builds needs the same platform layer: who is the user, what
can they do, which tenant are they in, what changed, and an admin surface to manage
it all. `devslab-kit` provides that layer once, override-friendly:

- **Auto-configured.** Add the starter, point it at PostgreSQL, boot.
- **Override-friendly.** Every default bean is `@ConditionalOnMissingBean` — replace
  any piece by declaring your own.
- **Contracts are Java APIs.** Each capability is split into a thin `-api` contract
  and a `-core` default; depend on the `-api` alone to supply your own implementation.

## Next steps

- [Installation](getting-started/installation.md) — add the dependency.
- [Quick Start](getting-started/quick-start.md) — boot a working app.
- [Configuration](reference/configuration.md) — every `devslab.kit.*` knob.
- [Admin REST API](reference/admin-api.md) — the `/admin/api/v1` surface.

The companion [**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui)
(Vue 3 + PrimeVue) is a ready-made console over the admin REST API.
