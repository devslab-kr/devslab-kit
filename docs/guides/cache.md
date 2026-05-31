# Caching

`devslab-kit` ships a **pluggable cache** behind Spring's `CacheManager`. You pick
the backend with one property; the kit wires the rest — including JSON
serialization for Redis, so you never implement `Serializable` or configure a
serializer. (Background: [ADR 0002](../adr/0002-distributed-cache.md).)

## Backends

| `cache.type` | `CacheManager` | Use for |
| --- | --- | --- |
| `in-memory` | `ConcurrentMapCacheManager` | Single-node apps and local dev (default). |
| `redis` | `RedisCacheManager` | Multiple replicas — entries are shared and correct across instances. |
| `none` | `NoOpCacheManager` | Disable caching entirely (every read recomputes). |

```yaml
devslab:
  kit:
    cache:
      type: redis
      ttl: PT10M
      key-prefix: "myapp:"
```

With `redis`, also point Spring at Redis (`spring.data.redis.*`).

## Using it

It's a standard Spring cache, so `@Cacheable` / `@CacheEvict` and an injected
`CacheManager` all work:

```java
@Service
class PriceService {

    @Cacheable("prices")
    public Price lookup(String sku) {
        // computed only on a miss; cached per the configured backend + TTL
        return expensiveLookup(sku);
    }
}
```

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
