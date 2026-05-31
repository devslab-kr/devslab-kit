package kr.devslab.kit.cache.core;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed {@link CacheManager} with kit-owned JSON serialization
 * (ADR 0002 §3). A consumer sets {@code devslab.kit.cache.type=redis}, adds
 * {@code spring-boot-starter-data-redis}, and never touches serialization —
 * values are stored as JSON, no {@code Serializable} requirement, no
 * {@code SerializationException}.
 *
 * <p>Values are serialized with {@link GenericJackson2JsonRedisSerializer}'s
 * no-arg constructor, which builds an {@code ObjectMapper} that embeds type
 * information so a cached {@code record} reads back as the same record (not a
 * {@code Map}). That decision lives here, in the kit, exactly once — consumers
 * configure nothing. Keys use {@link StringRedisSerializer} with the configured
 * prefix so entries are readable in {@code redis-cli}.
 *
 * <p>This class is named {@code DevslabKitRedisCacheConfiguration} — NOT
 * {@code RedisCacheConfiguration} — on purpose: Spring Data Redis's own
 * {@link RedisCacheConfiguration} (imported above) is used as the cache-defaults
 * builder, and giving this class the same simple name shadows that import and
 * scrambles type resolution inside the class body (javac then mis-sees the
 * value serializer's type). The distinct name keeps the import unambiguous.
 *
 * <p>Spring Data Redis 4.0 marks the serializer {@code @Deprecated(forRemoval)}
 * ahead of a Jackson-3 replacement not in the 4.0 line yet; the module compiles
 * with {@code -Xlint:-removal} for that reason (see its build file). This is the
 * single spot to swap it when the kit moves to that SDR line — the bean contract
 * doesn't change.
 *
 * <p>Guarded by {@link ConditionalOnClass} on {@link RedisConnectionFactory}:
 * with Redis off the classpath this whole configuration is skipped, so the
 * {@code compileOnly} Redis dependency never leaks onto an in-memory consumer. A
 * misconfigured {@code type=redis} with no Redis wired surfaces as the usual
 * missing-{@code RedisConnectionFactory} error (a constructor parameter) rather
 * than silently degrading.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "devslab.kit.cache", name = "type", havingValue = "redis")
class DevslabKitRedisCacheConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    CacheManager devslabKitRedisCacheManager(
            RedisConnectionFactory connectionFactory,
            CacheProperties properties
    ) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith(properties.getKeyPrefix());

        Duration ttl = properties.getTtl();
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            config = config.entryTtl(ttl);
        }
        if (!properties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
