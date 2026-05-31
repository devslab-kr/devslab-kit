package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Makes devslab-kit's JPA layer self-contained: a consumer can use the starter with a
 * plain {@code @SpringBootApplication} in any package and the kit's entities <em>and</em>
 * repositories are still discovered — no {@code @EntityScan} / {@code @EnableJpaRepositories}
 * / {@code scanBasePackages} required.
 *
 * <p>It does this with one additive move: {@link DevslabKitEntityPackagesRegistrar} adds
 * {@code kr.devslab.kit} to {@code AutoConfigurationPackages}. Spring Boot's own
 * {@code HibernateJpaAutoConfiguration} (entity scan) and
 * {@code DataJpaRepositoriesAutoConfiguration} (repository scan) both derive their base
 * packages from {@code AutoConfigurationPackages}, so widening it makes Boot discover the
 * kit's {@code @Entity} types and Spring Data repositories alongside the consumer's own.
 * The contribution is additive (the consumer's own package, registered by their plain
 * {@code @SpringBootApplication}, is preserved), so consumer entities/repositories keep
 * working.
 *
 * <p>Deliberately declares no {@code @EntityScan} (would <em>replace</em> the consumer's
 * entity package, since {@code JpaBaseConfiguration} only falls back to
 * {@code AutoConfigurationPackages} when {@code EntityScanPackages} is empty) and no
 * {@code @EnableJpaRepositories} (would register a {@code JpaRepositoryConfigExtension}
 * bean and make Boot's repository auto-config back off for the whole app). Widening
 * {@code AutoConfigurationPackages} avoids both footguns.
 *
 * <p>Ordered {@code before} Hibernate / Data-JPA auto-config (SB4-relocated class names)
 * so the package contribution lands before the {@code EntityManagerFactory} and the
 * repository scan are built.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@Import(DevslabKitEntityPackagesRegistrar.class)
public class PersistenceAutoConfiguration {
}
