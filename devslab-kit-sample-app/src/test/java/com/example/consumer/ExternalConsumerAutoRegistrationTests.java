package com.example.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import java.util.UUID;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves devslab-kit works for an external consumer whose {@code @SpringBootApplication}
 * lives <strong>outside</strong> {@code kr.devslab.kit} (here {@code com.example.consumer}),
 * with a plain annotation — the real-world scenario the kit's own sample-app (which
 * sits under the {@code kr.devslab.kit} prefix) cannot reproduce.
 *
 * <ul>
 *   <li>(a) the full kit context boots against Testcontainers Postgres,</li>
 *   <li>(b) a kit repository ({@link JpaPlatformUserAccountRepository}) is injectable and the
 *       kit entity is a managed type — the kit registered its own JPA layer, and</li>
 *   <li>(c) a consumer-defined {@code @Entity} + repository in {@code com.example.consumer}
 *       still work — the kit <em>broadened</em> scanning, it did not replace it
 *       (the {@code @EntityScan} / {@code @EnableJpaRepositories} footguns).</li>
 * </ul>
 *
 * <p>Bootstrap is off (no admin seeding needed) and the consumer's {@code consumer_widget}
 * table is created by Hibernate {@code ddl-auto=update}, so it coexists with the kit's
 * Flyway-managed {@code platform_*} tables without a migration.
 */
@Import(ConsumerTestcontainers.class)
@SpringBootTest
@TestPropertySource(properties = {
        "devslab.kit.bootstrap.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
class ExternalConsumerAutoRegistrationTests {

    @Autowired
    private JpaPlatformUserAccountRepository kitRepository;

    @Autowired
    private ConsumerWidgetRepository consumerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void kitRepositoryIsInjectableAndKitEntityIsManaged() {
        assertThat(kitRepository).isNotNull();
        // exercises the kit repo against real Postgres (kit's Flyway tables exist)
        assertThat(kitRepository.count()).isGreaterThanOrEqualTo(0L);

        Metamodel metamodel = entityManagerFactory.getMetamodel();
        assertThat(metamodel.entity(PlatformUserAccountEntity.class)).isNotNull();
    }

    @Test
    void consumerOwnEntityAndRepositoryStillWork() {
        Metamodel metamodel = entityManagerFactory.getMetamodel();
        // (c) consumer's @Entity must still be managed — proves no suppression
        assertThat(metamodel.entity(ConsumerWidget.class)).isNotNull();

        UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        consumerRepository.save(new ConsumerWidget(id, "widget-a"));
        assertThat(consumerRepository.findById(id)).isPresent();
        assertThat(consumerRepository.findById(id).get().getName()).isEqualTo("widget-a");
    }
}
