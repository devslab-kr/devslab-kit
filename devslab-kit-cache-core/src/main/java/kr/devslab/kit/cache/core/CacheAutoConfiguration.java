package kr.devslab.kit.cache.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Provides a {@link CacheManager} chosen by {@code devslab.kit.cache.type}
 * (ADR 0002) and turns on Spring's caching annotations — so a consumer flips
 * one property to get distributed caching and writes zero serializer config.
 *
 * <p>This class wires the {@code none} and {@code in-memory} backends. The
 * {@code redis} backend lives in {@link RedisCacheConfiguration}, imported here
 * but inert unless Redis is on the classpath (ADR 0002 §2–§3).
 *
 * <p>Everything is {@link ConditionalOnMissingBean} on {@link CacheManager}: a
 * consumer who defines their own cache manager always wins, and the kit's
 * {@link EnableCaching} stays out of the way too.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "devslab.kit.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
@org.springframework.context.annotation.Import(RedisCacheConfiguration.class)
public class CacheAutoConfiguration {

    /**
     * Enable Spring's caching annotations, but only when the kit is the one
     * providing the {@link CacheManager}. If the consumer manages caching
     * themselves (their own {@code CacheManager} bean), this stays inert so we
     * never double-enable or fight their setup.
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @EnableCaching
    @ConditionalOnMissingBean(CacheManager.class)
    static class KitCachingEnabled {
    }

    /**
     * {@code type: none} → a {@link NoOpCacheManager}. Caching annotations
     * resolve but never store, so every call runs the underlying method.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "devslab.kit.cache", name = "type", havingValue = "none")
    public CacheManager devslabKitNoOpCacheManager() {
        return new NoOpCacheManager();
    }

    /**
     * {@code type: in-memory} (default) → a {@link ConcurrentMapCacheManager}.
     * Single-JVM; see {@link CacheType#IN_MEMORY} for the multi-replica caveat.
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "devslab.kit.cache", name = "type", havingValue = "in-memory", matchIfMissing = true)
    public CacheManager devslabKitInMemoryCacheManager() {
        // Non-fixed cache names: created on first use, so consumers' own
        // @Cacheable("anything") works without pre-declaring cache names.
        return new ConcurrentMapCacheManager();
    }
}
