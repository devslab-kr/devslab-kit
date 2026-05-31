# Quick Start

Boot a working app with auth, multi-tenancy, an admin API and a seeded admin user.

## 1. Add the starter

See [Installation](installation.md).

## 2. Configure

Point the app at PostgreSQL (and optionally Redis), then set the platform knobs:

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
        secret: ${DEVSLAB_JWT_SECRET}   # 32+ bytes for HS256
        ttl: PT8H
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # provision the first admin on first boot
```

See the [Configuration reference](../reference/configuration.md) for every key.

## 3. Boot

On first start the kit:

1. runs Flyway to create the `platform_*` tables,
2. provisions a tenant, a `PLATFORM_ADMIN` role, the `admin.*` permissions and an
   admin user ([first-admin bootstrap](../guides/bootstrap.md)),
3. serves the admin REST API at `/admin/api/v1/**`.

```bash
./gradlew bootRun
```

## 4. Log in

Call the admin API directly, or point the
[admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) at it:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"<bootstrap password>"}'
```

A blank `bootstrap.admin-password` makes the kit generate a strong random one and
log it once at startup; set it explicitly for a known value.

## Next

- [Multi-tenancy](../guides/tenancy.md) · [Access (RBAC + ABAC)](../guides/access.md) · [Caching](../guides/cache.md)
- [Admin REST API](../reference/admin-api.md)
- A complete runnable setup lives in
  [`devslab-kit-sample-app`](https://github.com/devslab-kr/devslab-kit/tree/main/devslab-kit-sample-app).
