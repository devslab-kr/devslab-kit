package kr.devslab.kit.identity.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void matchesAfterHashing() {
        String hash = hasher.hash("hunter2");

        assertThat(hash).isNotEqualTo("hunter2");
        assertThat(hasher.matches("hunter2", hash)).isTrue();
        assertThat(hasher.matches("wrong", hash)).isFalse();
    }

    @Test
    void differentHashesForSameInput() {
        String h1 = hasher.hash("same");
        String h2 = hasher.hash("same");

        assertThat(h1).isNotEqualTo(h2);
        assertThat(hasher.matches("same", h1)).isTrue();
        assertThat(hasher.matches("same", h2)).isTrue();
    }

    @Test
    void matchesReturnsFalseForNullHash() {
        assertThat(hasher.matches("anything", null)).isFalse();
    }
}
