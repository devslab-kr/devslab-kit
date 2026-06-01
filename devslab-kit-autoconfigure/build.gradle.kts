description = "devslab-kit :: Spring Boot AutoConfiguration"

dependencies {
    api(project(":devslab-kit-core"))
    api(project(":devslab-kit-identity-api"))
    api(project(":devslab-kit-access-api"))
    api(project(":devslab-kit-tenant-api"))
    api(project(":devslab-kit-tenant-core"))
    api(project(":devslab-kit-identity-core"))
    api(project(":devslab-kit-access-core"))
    api(project(":devslab-kit-menu-api"))
    api(project(":devslab-kit-menu-core"))
    api(project(":devslab-kit-audit-api"))
    api(project(":devslab-kit-audit-core"))
    api(project(":devslab-kit-cache-api"))
    api(project(":devslab-kit-cache-core"))
    api(project(":devslab-kit-admin-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Flyway for the kit's own schema-history-table migration runner
    // (KitFlywayAutoConfiguration). compileOnly: it is @ConditionalOnClass(Flyway),
    // so it stays dormant unless the consumer puts Flyway on their classpath — which
    // any consumer migrating the kit's relational schema already does.
    compileOnly("org.flywaydb:flyway-core")

    // Micrometer for custom metrics; consumer's spring-boot-actuator activates it at runtime.
    compileOnly("io.micrometer:micrometer-core")

    // springdoc-openapi for the optional Swagger UI auto-configuration. compileOnly:
    // OpenApiAutoConfiguration is @ConditionalOnClass(GroupedOpenApi), so it stays
    // dormant unless the consumer puts springdoc on their own classpath. Version
    // pinned in gradle.properties (not managed by the Spring Boot BOM).
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-api:${property("SPRINGDOC_VERSION")}")
}

mavenPublishing {
    coordinates(
        providers.gradleProperty("GROUP").get(),
        "devslab-kit-autoconfigure",
        providers.gradleProperty("VERSION").get()
    )
    pom {
        name.set("devslab-kit-autoconfigure")
        description.set("devslab-kit :: Spring Boot AutoConfiguration")
        url.set(providers.gradleProperty("POM_URL"))
        inceptionYear.set(providers.gradleProperty("POM_INCEPTION_YEAR"))
        licenses {
            license {
                name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                url.set(providers.gradleProperty("POM_LICENSE_URL"))
                distribution.set(providers.gradleProperty("POM_LICENSE_DIST"))
            }
        }
        developers {
            developer {
                id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
                email.set(providers.gradleProperty("POM_DEVELOPER_EMAIL"))
                organization.set(providers.gradleProperty("POM_ORGANIZATION_NAME"))
                organizationUrl.set(providers.gradleProperty("POM_ORGANIZATION_URL"))
            }
        }
        organization {
            name.set(providers.gradleProperty("POM_ORGANIZATION_NAME"))
            url.set(providers.gradleProperty("POM_ORGANIZATION_URL"))
        }
        scm {
            url.set(providers.gradleProperty("POM_SCM_URL"))
            connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
        }
        issueManagement {
            system.set(providers.gradleProperty("POM_ISSUE_SYSTEM"))
            url.set(providers.gradleProperty("POM_ISSUE_URL"))
        }
    }
}
