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
 * <p>Two registrars, because entity scanning and repository scanning are wired differently:
 * <ul>
 *   <li>{@link DevslabKitEntityPackagesRegistrar} adds {@code kr.devslab.kit} to
 *       {@code AutoConfigurationPackages}, which is where Boot's Hibernate auto-config
 *       reads the {@code @Entity} packages to scan. The contribution is additive, so the
 *       consumer's own entity package (registered by their plain {@code @SpringBootApplication})
 *       is preserved.</li>
 *   <li>{@link DevslabKitJpaRepositoriesRegistrar} registers the kit's Spring Data
 *       repositories directly (subclassing Boot's own repository registrar). Registering any
 *       repository this way contributes {@code JpaRepositoryFactoryBean}s, which trips Boot's
 *       {@code @ConditionalOnMissingBean} guard and makes its {@code DataJpaRepositoriesAutoConfiguration}
 *       back off for the whole app — so this registrar scans <em>both</em> the kit root and the
 *       consumer's {@code AutoConfigurationPackages}, becoming the single repository scan that
 *       covers everything.</li>
 * </ul>
 *
 * <p>Deliberately declares neither {@code @EntityScan} (would <em>replace</em> the
 * consumer's entity package — {@code JpaBaseConfiguration} only falls back to
 * {@code AutoConfigurationPackages} when {@code EntityScanPackages} is empty) nor
 * {@code @EnableJpaRepositories} (would register a {@code JpaRepositoryConfigExtension}
 * bean and make Boot's repository auto-config back off for the whole app, dropping the
 * consumer's own repositories). The two registrars add the kit's types without suppressing
 * the consumer's.
 *
 * <p>Ordered {@code before} Hibernate / Data-JPA auto-config (SB4-relocated class names) so
 * the registrations land before the {@code EntityManagerFactory} and the repository scan
 * are built.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@Import({
        DevslabKitEntityPackagesRegistrar.class,
        DevslabKitJpaRepositoriesRegistrar.class
})
public class PersistenceAutoConfiguration {
}
