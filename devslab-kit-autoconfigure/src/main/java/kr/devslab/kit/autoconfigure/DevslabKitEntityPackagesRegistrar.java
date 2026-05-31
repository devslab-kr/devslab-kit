package kr.devslab.kit.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Adds {@code kr.devslab.kit} to the application's {@link AutoConfigurationPackages}
 * so Spring Boot discovers the kit's JPA {@code @Entity} types <em>and</em> Spring Data
 * repositories even when the consumer's {@code @SpringBootApplication} lives in an
 * unrelated package (e.g. {@code com.acme}). Both Boot's Hibernate entity scan and its
 * {@code DataJpaRepositoriesAutoConfiguration} repository scan derive their base packages
 * from {@code AutoConfigurationPackages}, so this one registration covers both.
 *
 * <p>Why this and not {@code @EntityScan} / {@code @EnableJpaRepositories}:
 * <ul>
 *   <li>{@code @EntityScan("kr.devslab.kit")} would <em>replace</em> the consumer's
 *       entity package — {@code JpaBaseConfiguration} only falls back to
 *       {@code AutoConfigurationPackages} when {@code EntityScanPackages} is empty.</li>
 *   <li>{@code @EnableJpaRepositories} would register a {@code JpaRepositoryConfigExtension}
 *       bean and make Boot's repository auto-config back off for the whole app.</li>
 * </ul>
 * {@link AutoConfigurationPackages#register} is instead <em>additive</em>: the consumer's
 * plain {@code @SpringBootApplication} already registered their package via
 * {@code @AutoConfigurationPackage}, and this merges {@code kr.devslab.kit} alongside it
 * (verified: it unions into a {@code LinkedHashSet}), so both the consumer's and the kit's
 * entities and repositories are picked up.
 */
class DevslabKitEntityPackagesRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, DevslabKitPackages.ROOT);
    }
}
