# Multi-tenancy

A **tenant** is an isolated workspace — one customer/org and all of its data. In
`devslab-kit` there is **always a tenant in context**, even in a single-tenant app: a
single-tenant deployment resolves a `default` tenant instead of skipping the abstraction,
so **your code is identical** whether you ship to one customer or thousands. You never
write a "no tenant" special case.

This guide assumes no prior knowledge. New here? Do the
[Tutorial](../getting-started/tutorial.md) first — Step 8 shows tenancy in a running app.

## Pick a mode

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    tenant:
      mode: single            # single | multi
      resolver: fixed         # fixed | header | jwt | subdomain
      default-tenant-id: default
```

| `mode` | When to use | Behaviour |
| --- | --- | --- |
| `single` | One customer / an internal tool | Every request resolves `default-tenant-id`. |
| `multi`  | A SaaS serving many customers | The **resolver** picks the tenant per request. |

Start with `single` + `fixed`. Switch to `multi` when you actually onboard a second
tenant — no code changes, only config.

## Resolvers (multi-tenant)

In `multi` mode the **resolver** decides whose request this is:

| `resolver` | Resolves the tenant from | Example |
| --- | --- | --- |
| `fixed` | always `default-tenant-id` | (the single-tenant default) |
| `header` | a request header (default `X-Tenant-Id`) | `X-Tenant-Id: acme` |
| `jwt` | the `tenant` claim on the kit-issued bearer token | the signed-in user's tenant |
| `subdomain` | the request host's subdomain | `acme.app.com` → `acme` |

!!! note "What the `jwt` resolver reads"
    It parses the **kit's own** bearer token (the one `/auth/login` issues, which
    carries a `tenant` claim) and falls back to `default-tenant-id` when there's no
    token — e.g. the login request itself. Validating *external* OAuth2 / OIDC tokens
    (JWKS, issuer checks, a configurable claim name) is a separate, larger concern not
    covered here; for that, supply a [custom resolver](#custom-resolver) below.

```yaml
devslab:
  kit:
    tenant:
      mode: multi
      resolver: header
      header: X-Tenant-Id     # only used by the header resolver
```

```bash
# with the header resolver, every request carries the tenant:
curl localhost:8080/api/invoices -H 'X-Tenant-Id: acme'
```

## Use it in your code

### Read the current tenant

`TenantContextHolder` holds the tenant bound to the current request (set by the kit
before your code runs):

```java
// src/main/java/com/example/myapp/InvoiceService.java
import kr.devslab.kit.tenant.TenantContextHolder;

@Service
class InvoiceService {

    private final TenantContextHolder tenantContext;
    private final InvoiceRepository invoices;

    InvoiceService(TenantContextHolder tenantContext, InvoiceRepository invoices) {
        this.tenantContext = tenantContext;
        this.invoices = invoices;
    }

    private String currentTenant() {
        return tenantContext.current()
                .orElseThrow(() -> new IllegalStateException("no tenant in context"))
                .tenantId().value();
    }

    List<Invoice> list() {
        return invoices.findByTenantId(currentTenant());   // never leak across tenants
    }

    Invoice create(String amount) {
        return invoices.save(new Invoice(UUID.randomUUID(), currentTenant(), amount));
    }
}
```

(There's also `TenantResolver` — inject it to resolve the tenant *outside* a web request,
e.g. in a scheduled job: `tenantResolver.resolve().tenantId().value()`.)

### Scope your data by tenant

The rule is simple: **put `tenant_id` on every tenant-owned entity and filter every query
by it.**

```java
// src/main/java/com/example/myapp/Invoice.java
@Entity
class Invoice {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    private String amount;
    // constructor + getters …
}
```

```java
// src/main/java/com/example/myapp/InvoiceRepository.java
interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByTenantId(String tenantId);
    Optional<Invoice> findByIdAndTenantId(UUID id, String tenantId);   // look-ups too
}
```

That's the whole pattern — identical in `single` and `multi` mode.

## Custom resolver

Need a strategy the built-ins don't cover (a DB lookup, header-or-path, an API key →
tenant map)? Declare your own `TenantResolver` bean and the kit's default backs off
(every kit bean is `@ConditionalOnMissingBean`):

```java
// src/main/java/com/example/myapp/ApiKeyTenantResolver.java
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.core.id.TenantId;

@Component
class ApiKeyTenantResolver implements TenantResolver {

    private final HttpServletRequest request;   // request-scoped
    private final TenantDirectory directory;     // your own lookup

    ApiKeyTenantResolver(HttpServletRequest request, TenantDirectory directory) {
        this.request = request;
        this.directory = directory;
    }

    @Override
    public TenantContext resolve() {
        String apiKey = request.getHeader("X-Api-Key");
        String tenantId = directory.tenantForApiKey(apiKey);   // e.g. a DB lookup
        return TenantContext.of(TenantId.of(tenantId));
    }
}
```

## Manage tenants

Create / suspend / archive tenants from the admin console's **Tenants** screen
(or the `tenants` REST endpoint) — see the
[Admin Console guide](admin-console.md#tenants).

See the [Configuration reference](../reference/configuration.md#tenant) for every key.
