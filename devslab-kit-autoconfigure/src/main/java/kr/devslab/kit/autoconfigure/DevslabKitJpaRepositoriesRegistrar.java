package kr.devslab.kit.autoconfigure;

import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.util.Streamable;

/**
 * Registers devslab-kit's Spring Data JPA repositories (in
 * {@code kr.devslab.kit.*.core.repository}) for any consumer, without forcing them
 * to add {@code @EnableJpaRepositories}.
 *
 * <p>Widening {@code AutoConfigurationPackages} (see {@link DevslabKitEntityPackagesRegistrar})
 * makes Hibernate manage the kit's {@code @Entity} types, but it does <em>not</em> make
 * Boot's {@code DataJpaRepositoriesAutoConfiguration} pick up the kit's repository
 * interfaces — its repository scan is driven from the importing application's package, not
 * from every entry in {@code AutoConfigurationPackages}. So the kit's repositories need an
 * explicit registration here.
 *
 * <p>This subclasses Boot's own {@link AbstractRepositoryConfigurationSourceSupport} — the
 * exact base its {@code DataJpaRepositoriesRegistrar} uses — and pins the scan to the kit
 * root via {@link #getBasePackages()}. It registers repository bean definitions directly
 * (not a {@code @EnableJpaRepositories} annotation), so it does <em>not</em> register the
 * {@code JpaRepositoryConfigExtension} bean that {@code DataJpaRepositoriesAutoConfiguration}
 * conditions on; Boot's own repository auto-config keeps running for the consumer's package.
 * Net: kit repositories (here) and consumer repositories (Boot) both register.
 */
class DevslabKitJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

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
        return Streamable.of(DevslabKitPackages.ROOT);
    }

    /**
     * Template carrying the {@code @EnableJpaRepositories} annotation metadata that
     * {@link AbstractRepositoryConfigurationSourceSupport} introspects. The scanned
     * packages come from {@link #getBasePackages()}, so the annotation's own
     * {@code basePackages} attribute is unused.
     */
    @EnableJpaRepositories
    private static final class EnableKitJpaRepositoriesConfiguration {
    }
}
