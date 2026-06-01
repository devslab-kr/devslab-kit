package kr.devslab.kit.autoconfigure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import kr.devslab.kit.admin.AdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Exposes the admin REST API through OpenAPI / Swagger UI with no consumer wiring —
 * add {@code springdoc-openapi-starter-webmvc-ui} to the classpath and
 * {@code /swagger-ui.html} (plus {@code /v3/api-docs}) come up, with the kit's
 * {@code /admin/api/v1/**} endpoints collected under one group.
 *
 * <p>Dormant unless springdoc is present: {@code @ConditionalOnClass(GroupedOpenApi.class)}
 * means the kit ships this config but contributes nothing until the consumer opts in
 * by adding the springdoc dependency (the kit declares it {@code compileOnly}). It can
 * be turned off without removing the dependency via
 * {@code devslab.kit.openapi.enabled=false} — typical in production, where the API docs
 * are usually not exposed.
 *
 * <p>Both beans are {@code @ConditionalOnMissingBean}, so a consumer can define their own
 * {@link OpenAPI} (e.g. to add security schemes or servers) or their own admin
 * {@link GroupedOpenApi} and the kit backs off.
 */
@AutoConfiguration
@EnableConfigurationProperties(DevslabKitProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(GroupedOpenApi.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.openapi",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenApiAutoConfiguration {

    /**
     * Groups every {@code /admin/api/v1/**} endpoint into one Swagger UI group so the
     * admin surface is browsable as a unit, separate from any of the consumer's own
     * controllers. Backs off if the consumer already declares a {@code GroupedOpenApi}.
     */
    @Bean
    @ConditionalOnMissingBean(name = "devslabKitAdminApiGroup")
    public GroupedOpenApi devslabKitAdminApiGroup(DevslabKitProperties props) {
        return GroupedOpenApi.builder()
                .group(props.getOpenApi().getAdminGroup())
                .pathsToMatch(AdminApiPaths.BASE + "/**")
                .build();
    }

    /**
     * Default OpenAPI document metadata (title + version). Only contributed if the
     * consumer hasn't defined their own {@link OpenAPI} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI devslabKitOpenApi(DevslabKitProperties props) {
        return new OpenAPI().info(new Info()
                .title(props.getOpenApi().getTitle())
                .version(props.getOpenApi().getVersion()));
    }
}
