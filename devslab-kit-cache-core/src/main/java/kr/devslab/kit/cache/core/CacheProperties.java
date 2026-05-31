package kr.devslab.kit.cache.core;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the kit's pluggable cache (ADR 0002), bound from
 * {@code devslab.kit.cache.*}.
 *
 * <p>The headline knob is {@link #type}: flip it to {@code redis} and a
 * single-node cache becomes a correct distributed one, with the kit owning
 * serialization so consumers never configure it.
 */
@ConfigurationProperties(prefix = "devslab.kit.cache")
public class CacheProperties {

    /** Master switch for the kit-provided CacheManager. */
    private boolean enabled = true;

    /** Backing store: {@code none}, {@code in-memory} (default), or {@code redis}. */
    private CacheType type = CacheType.IN_MEMORY;

    /**
     * Default time-to-live for cache entries. Applied by the Redis backend;
     * the in-memory backend ignores per-entry TTL (Spring's
     * {@code ConcurrentMapCacheManager} has no expiry) — documented limitation,
     * use {@code redis} when TTL matters.
     */
    private Duration ttl = Duration.ofMinutes(10);

    /** Key namespace prefix, so multiple apps can share one Redis instance safely. */
    private String keyPrefix = "devslab:";

    /** Whether to cache {@code null} return values. Off by default so a cached null doesn't mask a real miss. */
    private boolean cacheNullValues = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CacheType getType() {
        return type;
    }

    public void setType(CacheType type) {
        this.type = type;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public void setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
    }
}
