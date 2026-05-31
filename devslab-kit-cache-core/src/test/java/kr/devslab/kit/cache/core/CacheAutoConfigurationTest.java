package kr.devslab.kit.cache.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies the {@code devslab.kit.cache.type} switch wires the right
 * {@link CacheManager} and that a consumer-defined manager always wins
 * (ADR 0002 §2). No Redis here — that path needs a real Redis and lands with
 * its serialization test in PR 2.
 *
 * <p>Registers {@link CacheAutoConfiguration} as a plain user configuration
 * rather than via {@code AutoConfigurations.of(...)} on purpose: Spring Boot 4
 * ships the {@code AutoConfigurations} helper in a package that is split across
 * {@code spring-boot-autoconfigure} and {@code spring-boot-test-autoconfigure},
 * which fails to compile ("package not visible"). The condition semantics under
 * test still hold; for the one order-sensitive case ({@link #consumerCacheManagerWins})
 * the consumer config is registered <em>before</em> the auto-config so
 * {@code @ConditionalOnMissingBean} sees it.
 */
class CacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CacheAutoConfiguration.class);

    @Test
    void defaultsToInMemory() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(CacheManager.class);
            assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(ConcurrentMapCacheManager.class);
        });
    }

    @Test
    void inMemoryWhenRequestedExplicitly() {
        runner.withPropertyValues("devslab.kit.cache.type=in-memory").run(ctx ->
                assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(ConcurrentMapCacheManager.class));
    }

    @Test
    void noOpWhenTypeNone() {
        runner.withPropertyValues("devslab.kit.cache.type=none").run(ctx ->
                assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class));
    }

    @Test
    void backsOffEntirelyWhenDisabled() {
        runner.withPropertyValues("devslab.kit.cache.enabled=false").run(ctx ->
                assertThat(ctx).doesNotHaveBean(CacheManager.class));
    }

    @Test
    void consumerCacheManagerWins() {
        // Consumer config registered FIRST so the auto-config's
        // @ConditionalOnMissingBean(CacheManager) sees it and backs off.
        new ApplicationContextRunner()
                .withUserConfiguration(CustomCacheManagerConfig.class, CacheAutoConfiguration.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheManager.class);
                    assertThat(ctx).hasBean("myCustomCacheManager");
                    assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(NoOpCacheManager.class);
                });
    }

    @Test
    void propertiesBind() {
        runner.withPropertyValues(
                "devslab.kit.cache.ttl=PT30M",
                "devslab.kit.cache.key-prefix=app:",
                "devslab.kit.cache.cache-null-values=true"
        ).run(ctx -> {
            CacheProperties props = ctx.getBean(CacheProperties.class);
            assertThat(props.getTtl()).hasMinutes(30);
            assertThat(props.getKeyPrefix()).isEqualTo("app:");
            assertThat(props.isCacheNullValues()).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCacheManagerConfig {
        @Bean
        CacheManager myCustomCacheManager() {
            // A distinctly-typed manager (NoOp) so the test can tell it apart
            // from the kit's default ConcurrentMapCacheManager.
            return new NoOpCacheManager();
        }
    }
}
