# ADR 0002 — Pluggable distributed cache (one property, zero serializer pain)

- **Status:** Accepted
- **Date:** 2026-05-31 (accepted 2026-05-31)
- **Languages:** [English](0002-distributed-cache.md) · [한국어](0002-distributed-cache.ko.md)
- **Implemented by:** `devslab-kit-cache-api` + `devslab-kit-cache-core` —
  `CacheProperties` (`devslab.kit.cache.*`), `CacheAutoConfiguration` (the
  `type` switch + guarded `@EnableCaching`, none/in-memory backends). The Redis
  backend + JSON serialization (PR 2), the menu-cache migration (PR 3), and the
  sample-app/docs (PR 4) follow per the implementation plan below.

## Context

`devslab-kit` already caches one thing — the per-user menu tree — via a small
hand-rolled `CachingMenuProvider` backed by a `ConcurrentHashMap` (ADR 0001 era).
That works for a single instance, but the moment a consumer runs more than one
replica it breaks down: each instance has its own map, so a menu edit (or any
cached value) goes stale on every node except the one that handled the write,
until each node's TTL independently lapses.

The deeper point, raised while reviewing the kit for its first public release:
**setting up a correct distributed cache in Spring Boot is fiddly, and every
consumer pays the same tax.** They have to pick a `CacheManager`, wire a
`RedisConnectionFactory`, and — the part that bites everyone — configure
serialization. The default JDK serializer requires every cached type to be
`Serializable`, produces opaque binary blobs, and throws
`SerializationException` at runtime the first time someone caches a record or a
type that forgot the marker interface. Switching to JSON means hand-configuring
`GenericJackson2JsonRedisSerializer`, deciding on default-typing so polymorphic
values round-trip, and getting the `ObjectMapper` right.

A platform starter is exactly the right place to make that decision **once**, so
no downstream product ever has to. The goal:

> A consumer flips **one property** to turn the cache from in-memory to
> distributed (Redis), and **never touches serialization**. Their own
> `@Cacheable` methods just work across replicas.

## Decision

### 1. A `devslab.kit.cache.*` block with a `type` switch

```yaml
devslab.kit.cache:
  type: in-memory        # none | in-memory | redis   (default: in-memory)
  ttl: PT10M             # default entry TTL
  key-prefix: "devslab:" # namespace keys in shared Redis
  null-values: false     # cache nulls? (default no — avoids masking misses)
```

- **`in-memory`** (default) — a `ConcurrentMapCacheManager`-style manager. No
  external dependency; same single-instance behaviour we have today.
- **`redis`** — a `RedisCacheManager` with JSON serialization fully
  pre-configured by the kit (see §3). The consumer adds Redis connection
  settings (`spring.data.redis.*`) and nothing else.
- **`none`** — a `NoOpCacheManager`. Caching annotations become pass-throughs;
  useful in tests that must see writes immediately.

### 2. The kit owns the `CacheManager` bean, conditionally

A new `CacheAutoConfiguration` provides a `CacheManager` keyed on
`devslab.kit.cache.type`, each guarded so it only activates when its backing
classes are present:

- `redis` requires `RedisConnectionFactory` on the classpath
  (`@ConditionalOnClass`) **and** a connection factory bean
  (`@ConditionalOnBean`). If `type=redis` but Redis isn't wired, the kit fails
  fast with a clear message rather than silently degrading to in-memory.
- All beans are `@ConditionalOnMissingBean(CacheManager.class)` — a consumer
  who defines their own `CacheManager` always wins. The kit never fights an
  explicit choice.
- `@EnableCaching` is switched on by the kit so consumers don't have to
  remember it (also `@ConditionalOnMissingBean(CacheManager.class)`-guarded so
  it's inert if they manage caching themselves).

### 3. Serialization is solved once, in the kit

For the `redis` path the kit configures `RedisCacheConfiguration` with:

- **Values:** `GenericJackson2JsonRedisSerializer` over a kit-owned
  `ObjectMapper` — JSON, human-readable in `redis-cli`, no `Serializable`
  requirement. Default typing is enabled in a **safe** form
  (`activateDefaultTyping` restricted to non-final types via an allow-list
  validator, not the CVE-prone `LaissezFaireSubTypeValidator`) so polymorphic
  and generic values round-trip without the consumer thinking about it.
- **Keys:** `StringRedisSerializer` with the configured `key-prefix`.
- **TTL & null handling:** from the property block.

This is the crux of the value proposition: the consumer caches a Java record and
reads it back as the same record on another node, and never sees a
`SerializationException` or writes a serializer config line.

### 4. Module placement

Follow the existing `-api` / `-core` split:

- **`devslab-kit-cache-api`** — the small contract: a `CacheNames` constants
  holder, and any kit-level cache abstraction we want stable. Minimal; most
  consumers just use Spring's `@Cacheable`.
- **`devslab-kit-cache-core`** — `CacheAutoConfiguration` lives in
  `devslab-kit-autoconfigure` (where the other auto-configs are), but the
  Redis-specific config + the `ObjectMapper`/serializer wiring live here so the
  Redis classes are an optional, isolated dependency.

Spring's `spring-boot-starter-data-redis` is declared `optional`/`compileOnly`
in `-cache-core` so it's only on the classpath when the consumer opts in. The
starter (`devslab-kit-spring-boot-starter`) pulls in `-cache-core`; Redis itself
is **not** a transitive dependency — the consumer adds
`spring-boot-starter-data-redis` when they set `type=redis`. README documents
this one-liner.

### 5. Menu cache becomes the first consumer

The existing `CachingMenuProvider` is re-expressed on top of the shared cache:

- `type=in-memory`/`redis` → the menu provider delegates to the kit
  `CacheManager` (a `@Cacheable`-style read on a `menu:<userId>` key) instead of
  its private map. One cache story, not two.
- `type=none` → the menu provider skips caching, replacing today's
  "zero/negative TTL disables the decorator" special case.
- `devslab.kit.menu.cache-ttl` is kept as an optional **per-cache override**;
  when unset it inherits `devslab.kit.cache.ttl`. Existing consumers see no
  behaviour change at the default.

### 6. What consumers get for free

Because the kit turns on `@EnableCaching` and owns a correctly-serializing
`CacheManager`, a consumer's **own** code can annotate methods with
`@Cacheable("their-cache")` and immediately get distributed caching with no
further setup — the headline benefit. Their domain types cache as JSON, across
replicas, with the kit's serialization policy.

## Consequences

**Positive**
- One property (`type: redis`) turns a single-node cache into a correct
  distributed one. No serializer config, no `Serializable`, ever.
- The menu cache stops being a multi-replica correctness bug.
- The benefit extends to the consumer's own `@Cacheable` usage — a real
  platform feature, not just an internal optimization.
- Redis stays optional and non-transitive; in-memory default keeps the
  zero-dependency story for simple deploys.

**Negative / cost**
- New modules + an auto-config + a carefully-configured `ObjectMapper`. Default
  typing must be done with a safe validator — this is the one place to get
  security right (no `LaissezFaireSubTypeValidator`).
- Caching the menu through Spring's `CacheManager` loses the bespoke
  `invalidate(userId)` method unless we expose an equivalent; admin menu edits
  need a documented eviction path (`@CacheEvict` or a `Cache.evict` call).
- One more thing the starter turns on by default (`@EnableCaching`); guarded by
  `@ConditionalOnMissingBean` but worth calling out.

## Implementation plan (PR breakdown)

1. **`-cache-api` + `-cache-core` + `CacheAutoConfiguration`** — the `type`
   switch, in-memory + none managers, property block, `@EnableCaching` guarded.
   No Redis yet. Unit tests for the conditional wiring.
2. **Redis path + serialization** — `RedisCacheManager`, the safe-default-typing
   `ObjectMapper`, key prefix/TTL. Real-Redis Testcontainers test proving a
   record round-trips across a fresh `CacheManager` (serialization is the risk).
3. **Migrate the menu cache** onto the shared `CacheManager`; keep the
   per-cache TTL override; preserve admin eviction. Regression-test menu caching
   under all three `type` values.
4. **sample-app + docs** — sample-app shows `type=redis` with the compose Redis
   it already starts (so Redis stops being dead weight); README documents the
   one-property switch + the "your own @Cacheable just works" benefit.

## Alternatives considered

- **Leave the hand-rolled map.** Rejected — breaks on >1 replica, and every
  consumer re-solves distributed caching themselves, which is the tax this kit
  exists to remove.
- **Caffeine for in-memory, Redis for distributed, two code paths.** Rejected
  for v1 — Spring's `CacheManager` abstraction already lets us swap backends
  behind one interface; a `ConcurrentMapCacheManager` is enough for the
  in-memory default. Caffeine can slot in later behind the same `type` switch
  (`type: caffeine`) without an API change.
- **Make Redis a hard transitive dependency.** Rejected — forces the Redis jar
  (and a Redis server expectation) on consumers who just want the in-memory
  default. Keeping it optional preserves the zero-dependency simple path.
- **Let consumers configure their own serializer.** Rejected — that's exactly
  the pain this ADR removes. The kit's whole value here is deciding it once.
