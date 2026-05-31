package kr.devslab.kit.cache.core;

/**
 * Backing store for the kit's {@code CacheManager}, selected via
 * {@code devslab.kit.cache.type}. See ADR 0002.
 */
public enum CacheType {

    /**
     * No-op caching. Annotations become pass-throughs; every call hits the
     * underlying method. Useful in tests that must observe writes immediately.
     */
    NONE,

    /**
     * Single-JVM {@code ConcurrentMapCacheManager}. No external dependency.
     * Correct for a single instance; goes stale across replicas (each JVM has
     * its own map) — use {@link #REDIS} when running more than one node.
     */
    IN_MEMORY,

    /**
     * Distributed {@code RedisCacheManager} with kit-owned JSON serialization
     * (ADR 0002 §3). Requires {@code spring-boot-starter-data-redis} on the
     * classpath and a configured connection; the kit fails fast otherwise.
     */
    REDIS
}
