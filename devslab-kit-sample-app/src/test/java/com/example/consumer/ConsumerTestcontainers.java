package com.example.consumer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres + Redis for the external-consumer test. Declared in
 * {@code com.example.consumer} (not reusing the sample-app's package-private
 * {@code TestcontainersConfiguration}) so the whole scenario stays inside a single
 * non-{@code kr.devslab.kit} package — a faithful stand-in for a real consumer.
 */
@TestConfiguration(proxyBeanMethods = false)
class ConsumerTestcontainers {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
    }
}
