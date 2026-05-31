package com.example.consumer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A consumer-defined Spring Data repository in the consumer's own package. It
 * being injectable proves the kit did <em>not</em> disable Boot's
 * {@code DataJpaRepositoriesAutoConfiguration} for the consumer (the
 * {@code @EnableJpaRepositories} footgun).
 */
public interface ConsumerWidgetRepository extends JpaRepository<ConsumerWidget, UUID> {
}
