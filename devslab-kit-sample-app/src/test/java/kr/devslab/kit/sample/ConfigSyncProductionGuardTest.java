package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.devslab.kit.autoconfigure.ConfigSyncProductionGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * ADR 0003 §5 — config sync must fail-fast (not silently disable) if it is enabled while a
 * production profile is active. Pure unit test of the guard; no Spring context needed.
 */
class ConfigSyncProductionGuardTest {

    @Test
    void failsFastWhenEnabledUnderProductionProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        assertThatThrownBy(new ConfigSyncProductionGuard(env)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("config-sync")
                .hasMessageContaining("prod");
    }

    @Test
    void allowsNonProductionProfiles() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThatNoException()
                .isThrownBy(new ConfigSyncProductionGuard(env)::afterPropertiesSet);
    }

    @Test
    void allowsWhenNoActiveProfile() {
        assertThatNoException()
                .isThrownBy(new ConfigSyncProductionGuard(new MockEnvironment())::afterPropertiesSet);
    }
}
