package kr.devslab.kit.autoconfigure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import kr.devslab.kit.identity.event.LoginFailedEvent;
import kr.devslab.kit.identity.event.LoginSucceededEvent;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;

/**
 * Custom Micrometer metrics for devslab-kit (planning §13).
 *
 * <p>Listens to identity events and increments per-vertical counters so
 * that any consumer that exposes Spring Boot Actuator can read them via
 * {@code /actuator/metrics/{name}}.
 *
 * <p>Counters registered lazily on first event so we don't need to know
 * up front what failure reasons / tenant ids exist.
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class MetricsAutoConfiguration {

    private static final String METRIC_LOGIN_SUCCESS = "devslab.identity.login.success";
    private static final String METRIC_LOGIN_FAILURE = "devslab.identity.login.failure";

    private final MeterRegistry meterRegistry;

    public MetricsAutoConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void onLoginSucceeded(LoginSucceededEvent event) {
        Counter.builder(METRIC_LOGIN_SUCCESS)
                .tag("tenant", event.tenantId().value())
                .register(meterRegistry)
                .increment();
    }

    @EventListener
    public void onLoginFailed(LoginFailedEvent event) {
        Counter.builder(METRIC_LOGIN_FAILURE)
                .tag("tenant", event.tenantId().value())
                .tag("reason", event.reason().name())
                .register(meterRegistry)
                .increment();
    }
}
