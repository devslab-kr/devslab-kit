# Multi-tenancy

`devslab-kit` always runs inside a **tenant context** — even single-tenant
deployments resolve a default tenant rather than skipping the abstraction, so your
code never special-cases "no tenant".

## Modes

| `tenant.mode` | Behaviour |
| --- | --- |
| `single` | One tenant (`tenant.default-tenant-id`). The resolver always returns it. |
| `multi` | The tenant is resolved per request by the chosen resolver. |

## Resolvers

Pick one with `tenant.resolver`:

| Resolver | Resolves the tenant from |
| --- | --- |
| `fixed` | Always `tenant.default-tenant-id` (the single-tenant default). |
| `header` | A request header (e.g. `X-Tenant-Id`). |
| `jwt` | A claim on the authenticated JWT. |
| `subdomain` | The request host's subdomain (`acme.app.com` → `acme`). |

```yaml
devslab:
  kit:
    tenant:
      mode: multi
      resolver: header
```

## Using it

Inject `TenantResolver` to resolve the active tenant (or `TenantContextHolder` to
read the one bound to the current request):

```java
import kr.devslab.kit.tenant.TenantResolver;

@Service
class ReportService {
    private final TenantResolver tenants;

    ReportService(TenantResolver tenants) { this.tenants = tenants; }

    void run() {
        String tenantId = tenants.resolve().tenantId().value();
        // … scope your query to tenantId …
    }
}
```

## Override

Need a custom resolution strategy (a database lookup, a composite of header +
path, …)? Declare your own `TenantResolver` bean and the kit's default backs off:

```java
@Bean
TenantResolver tenantResolver() {
    return () -> /* your TenantContext */;
}
```

See the [Configuration reference](../reference/configuration.md#tenant) for all keys.
