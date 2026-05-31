package kr.devslab.kit.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the headline claim of ADR 0002: with {@code type=redis} a consumer's
 * value round-trips through real Redis as JSON with its concrete type intact —
 * no {@code Serializable}, no serializer config, no {@code SerializationException}.
 *
 * <p>Runs against a real Redis (Testcontainers) because serialization is the
 * whole risk; a mock would prove nothing. Drives the kit's own
 * {@link RedisCacheConfiguration} bean methods directly with a real
 * {@link LettuceConnectionFactory} — deliberately bypassing Spring Boot's
 * autoconfiguration, whose {@code RedisAutoConfiguration} is deprecated for
 * removal in Spring Boot 4 and would trip this module's {@code -Werror}. The
 * config class is package-private; this test shares its package.
 */
@Testcontainers
class RedisCacheRoundTripTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static CacheManager cacheManager;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        DevslabKitRedisCacheConfiguration config = new DevslabKitRedisCacheConfiguration();
        // Call the autoconfig bean method directly (config class is package-private).
        cacheManager = config.devslabKitRedisCacheManager(connectionFactory, new CacheProperties());
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    /** A record — the classic type JDK serialization / a forgotten Serializable trips on. */
    record Customer(String id, String name, int visits) {
    }

    @Test
    void buildsRedisCacheManager() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void recordRoundTripsThroughRealRedisWithItsConcreteType() {
        Cache cache = cacheManager.getCache("customers");
        assertThat(cache).isNotNull();

        Customer original = new Customer("c-1", "Aisha", 7);
        cache.put("c-1", original);

        // Read back via a fresh get — deserialized from Redis bytes, not a local ref.
        Customer fromCache = cache.get("c-1", Customer.class);
        assertThat(fromCache).isEqualTo(original);
        assertThat(fromCache.name()).isEqualTo("Aisha");
        assertThat(fromCache.visits()).isEqualTo(7);
    }

    @Test
    void stringRoundTrips() {
        Cache cache = cacheManager.getCache("strings");
        assertThat(cache).isNotNull();
        cache.put("k", "hello-redis");
        assertThat(cache.get("k", String.class)).isEqualTo("hello-redis");
    }

    @Test
    void missReturnsNull() {
        Cache cache = cacheManager.getCache("customers");
        assertThat(cache).isNotNull();
        assertThat(cache.get("does-not-exist")).isNull();
    }
}
