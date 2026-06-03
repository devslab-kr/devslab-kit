# Caching

A **cache** stores the result of an expensive call so the next identical call returns
instantly instead of recomputing. `devslab-kit` ships a **pluggable cache** behind
Spring's `CacheManager`: you pick the backend with **one property**, and the kit wires
the rest — including JSON serialization for Redis, so you never implement
`Serializable` or configure a serializer. (Background: [ADR 0002](../adr/0002-distributed-cache.md).)

New here? Do the [Tutorial](../getting-started/tutorial.md) first. This guide assumes
you have a running app.

## Backends

| `cache.type` | `CacheManager` | Use for |
| --- | --- | --- |
| `in-memory` | `ConcurrentMapCacheManager` | Single-node apps and local dev (default). |
| `redis` | `RedisCacheManager` | Multiple replicas — entries are shared and correct across instances. |
| `none` | `NoOpCacheManager` | Disable caching entirely (every read recomputes). |

The rule of thumb: **`in-memory` until you run more than one instance**, then `redis`
so a value cached by one replica is seen by the others.

## Configure

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    cache:
      type: redis
      ttl: PT10M
      key-prefix: "myapp:"

# the redis backend also needs Spring pointed at Redis:
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

With `type: in-memory` (the default) you need none of the `spring.data.redis` block.

## Use it

It's a standard Spring cache, so `@Cacheable` / `@CacheEvict` and an injected
`CacheManager` all work:

```java
// src/main/java/com/example/myapp/PriceService.java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Service
class PriceService {

    @Cacheable("prices")                 // computed only on a miss, then cached
    public Price lookup(String sku) {
        return expensiveLookup(sku);     // e.g. a slow DB or upstream call
    }

    @CacheEvict(value = "prices", key = "#sku")   // drop one entry on change
    public void priceChanged(String sku) {
        // next lookup(sku) recomputes and re-caches
    }
}
```

The first `lookup("ABC")` runs the method; the second returns the cached `Price`
without entering the method body, until the TTL expires or `priceChanged("ABC")`
evicts it.

!!! tip "Confirm it's actually caching"
    Log inside `expensiveLookup` — it should print on the **first** call only. With
    `redis`, you can also watch the keys appear: `redis-cli KEYS 'myapp:prices*'`.

The kit's own per-user [menu](menus.md) tree rides this same cache manager.

## Redis serialization

The Redis backend owns serialization: values are stored as JSON, with safe
polymorphic typing restricted to an allow-list (`java.*` plus
`cache.allowed-package`, default `kr.devslab`). That means:

- no `implements Serializable` on your cached types,
- no serializer beans to register,
- readable values in `redis-cli`.

Keep cached types to your own packages (or widen `cache.allowed-package`).

## Tuning

| Key | Default | |
| --- | --- | --- |
| `cache.type` | `in-memory` | Backend selector. |
| `cache.ttl` | `PT10M` | Entry TTL (Redis). |
| `cache.key-prefix` | `devslab:` | Redis key namespace. |
| `cache.allowed-package` | `kr.devslab` | Allow-list for polymorphic JSON typing. |

See the [Configuration reference](../reference/configuration.md#cache).
