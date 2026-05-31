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
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Redis-backed {@link CacheManager} with kit-owned JSON serialization
 * (ADR 0002 §3). A consumer sets {@code devslab.kit.cache.type=redis}, adds
 * {@code spring-boot-starter-data-redis}, and never touches serialization —
 * values are stored as JSON, no {@code Serializable} requirement, no
 * {@code SerializationException}.
 *
 * <p>Values are serialized with {@link GenericJacksonJsonRedisSerializer} — the
 * Jackson-3 generic serializer (Spring Boot 4 is on Jackson 3 / {@code tools.jackson};
 * the older {@code GenericJackson2JsonRedisSerializer} is Jackson-2 based and
 * deprecated in Spring Data Redis 4, so it's deliberately not used). The
 * serializer is built with default typing enabled so type hints are embedded in
 * the JSON: that's what lets a cached {@code record} read back as the same
 * record rather than a {@code Map} (Spring's {@code RedisCache} deserializes
 * without a target type, so the type has to live in the payload).
 *
 * <p>Default typing is the CVE-sensitive part of JSON deserialization, so it's
 * constrained by a {@link BasicPolymorphicTypeValidator} allow-list — never the
 * unsafe/laissez-faire variant. The allow-list covers {@code java.*} (the
 * component types of collections, records, dates) plus the consumer's base
 * package, taken from {@code devslab.kit.cache.allowed-package} (default
 * {@code kr.devslab}). A consumer caching their own types sets that property to
 * their root package; if they need finer control they can define their own
 * {@code CacheManager} bean and the kit backs off entirely.
 *
 * <p>This class is named {@code DevslabKitRedisCacheConfiguration} — NOT
 * {@code RedisCacheConfiguration} — on purpose: Spring Data Redis's own
 * {@link RedisCacheConfiguration} (imported above) is the cache-defaults
 * builder, and a same-name class would shadow that import.
 *
 * <p>Guarded by {@link ConditionalOnClass} on {@link RedisConnectionFactory}:
 * with Redis off the classpath this whole configuration is skipped, so the
 * {@code compileOnly} Redis dependency never leaks onto an in-memory consumer.
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
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("java.")
                .allowIfSubType(properties.getAllowedPackage())
                .build();

        // Declared as RedisSerializer<Object> (a plain widening conversion —
        // GenericJacksonJsonRedisSerializer implements RedisSerializer<Object>)
        // so SerializationPair.fromSerializer infers the value type cleanly.
        RedisSerializer<Object> valueSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(validator)
                        .build();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
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
