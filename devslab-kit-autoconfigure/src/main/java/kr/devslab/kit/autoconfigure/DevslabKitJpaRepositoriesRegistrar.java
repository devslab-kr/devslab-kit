package kr.devslab.kit.autoconfigure;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.util.Streamable;

/**
 * Registers devslab-kit's Spring Data JPA repositories (in
 * {@code kr.devslab.kit.*.core.repository}) for any consumer, without forcing them to add
 * {@code @EnableJpaRepositories} — and without dropping the consumer's own repositories.
 *
 * <p><strong>Why this scans both the kit and the consumer.</strong> Subclassing Boot's own
 * {@link AbstractRepositoryConfigurationSourceSupport} and registering repository definitions
 * necessarily contributes a {@code JpaRepositoryFactoryBean} per discovered repository. Boot's
 * {@code DataJpaRepositoriesAutoConfiguration} is guarded by
 * {@code @ConditionalOnMissingBean({JpaRepositoryFactoryBean.class, JpaRepositoryConfigExtension.class})}
 * (verified against Spring Boot 4.0.6 source, line 80), so the instant this registrar runs — it is
 * ordered <em>before</em> that auto-config — Boot's own repository scan backs off for the whole
 * application. This registrar therefore becomes the single repository scan and must cover the
 * consumer's packages as well as the kit's. Pinning it to {@code kr.devslab.kit} only (the original
 * bug) made the surviving scan kit-only and silently dropped the consumer's repositories.
 *
 * <p><strong>Why {@code getBasePackages()} is overridden rather than inherited.</strong> Boot's
 * default returns {@code AutoConfigurationPackages.get(beanFactory)}. That alone is not reliable
 * here: the kit root is added to {@code AutoConfigurationPackages} by
 * {@link DevslabKitEntityPackagesRegistrar}, but the {@code BasePackages} singleton can be
 * instantiated (and frozen) at repository-scan time before that contribution is visible — observed
 * directly: at this point {@code AutoConfigurationPackages.get(...)} returns only the consumer's
 * package. So this explicitly unions the kit root with whatever {@code AutoConfigurationPackages}
 * reports (which always includes the consumer's own {@code @SpringBootApplication} package), giving
 * a scan that covers both regardless of that timing. (The entity side is unaffected: Hibernate reads
 * {@code AutoConfigurationPackages} later, by which point the kit root is present — proven by the
 * external-consumer test's kit-entity round-trip.)
 *
 * <p>The {@code com.example.consumer} {@code ExternalConsumerAutoRegistrationTests} guards both
 * halves: a kit repository injectable from an unrelated package, and the consumer's own repository
 * still discovered.
 */
class DevslabKitJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableJpaRepositories.class;
    }

    @Override
    protected Class<?> getConfiguration() {
        return EnableKitJpaRepositoriesConfiguration.class;
    }

    @Override
    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new JpaRepositoryConfigExtension();
    }

    @Override
    protected Streamable<String> getBasePackages() {
        Set<String> packages = new LinkedHashSet<>();
        packages.add(DevslabKitPackages.ROOT);
        if (this.beanFactory != null && AutoConfigurationPackages.has(this.beanFactory)) {
            packages.addAll(AutoConfigurationPackages.get(this.beanFactory));
        }
        return Streamable.of(packages);
    }

    /**
     * Template carrying the {@code @EnableJpaRepositories} annotation metadata that
     * {@link AbstractRepositoryConfigurationSourceSupport} introspects (mirrors Boot's own
     * {@code DataJpaRepositoriesRegistrar.EnableJpaRepositoriesConfiguration}). The scanned
     * packages come from {@link #getBasePackages()}, so the annotation's own
     * {@code basePackages} attribute is unused.
     */
    @EnableJpaRepositories
    private static final class EnableKitJpaRepositoriesConfiguration {
    }
}
