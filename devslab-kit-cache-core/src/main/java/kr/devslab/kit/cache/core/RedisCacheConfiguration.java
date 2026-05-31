package kr.devslab.kit.cache.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis-backed {@code CacheManager} with kit-owned JSON serialization
 * (ADR 0002 §3) — a consumer sets {@code type: redis}, adds
 * {@code spring-boot-starter-data-redis}, and never touches serialization.
 *
 * <p><strong>PR 1 placeholder.</strong> This class is intentionally a no-op
 * scaffold right now: it carries the {@link ConditionalOnClass} /
 * {@link ConditionalOnProperty} guards so the wiring is in place, but the actual
 * {@code RedisCacheManager} + the safe-default-typing {@code ObjectMapper}
 * (the part that needs a real-Redis Testcontainers round-trip test) lands in
 * PR 2. Until then, {@code type: redis} resolves no {@code CacheManager} here —
 * documented so we don't ship a half-built serializer.
 *
 * <p>Guarded by {@link ConditionalOnClass} on {@link RedisConnectionFactory}:
 * when Redis isn't on the classpath this whole configuration is skipped, so the
 * {@code compileOnly} Redis dependency never leaks onto a consumer who stays
 * in-memory.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "devslab.kit.cache", name = "type", havingValue = "redis")
class RedisCacheConfiguration {

    // PR 2:
    //   @Bean @ConditionalOnMissingBean(CacheManager.class)
    //   @ConditionalOnBean(RedisConnectionFactory.class)
    //   CacheManager devslabKitRedisCacheManager(RedisConnectionFactory cf, CacheProperties props) { ... }
    // with GenericJackson2JsonRedisSerializer over a kit ObjectMapper using a
    // SAFE default-typing validator (allow-list, not LaissezFaireSubTypeValidator).
    //
    // The unused imports below are referenced in the PR-2 signature; keeping the
    // guard annotations active now means the conditional contract is testable.
    @SuppressWarnings("unused")
    private static final Class<?> GUARD_MISSING = ConditionalOnMissingBean.class;
}
