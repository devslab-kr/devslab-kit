package kr.devslab.kit.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * whole risk; a mock would prove nothing. The connection factory is created and
 * started <em>per test</em> ({@link BeforeEach}), not once in a static
 * {@code @BeforeAll}: a {@link LettuceConnectionFactory} built in a static
 * context doesn't reliably finish its async client init before the cache's first
 * write, which silently drops it (an isolated diagnostic confirmed the
 * serializer + cache + Redis I/O are otherwise correct). Per-test setup gives
 * each test a freshly-started, fully-initialized factory.
 *
 * <p>Drives the kit's {@link DevslabKitRedisCacheConfiguration} bean method
 * directly with a real {@link LettuceConnectionFactory} — deliberately bypassing
 * Spring Boot's autoconfiguration. The config class is package-private; this
 * test shares its package.
 */
@Testcontainers
class RedisCacheRoundTripTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        cacheManager = new DevslabKitRedisCacheConfiguration()
                .devslabKitRedisCacheManager(connectionFactory, new CacheProperties());
    }

    @AfterEach
    void tearDown() {
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
        assertThat(cache.get("absent-key-" + System.nanoTime())).isNull();
    }
}
