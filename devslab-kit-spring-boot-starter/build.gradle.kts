description = "devslab-kit :: Spring Boot starter (dependency bundle)"

dependencies {
    api(project(":devslab-kit-autoconfigure"))
    api(project(":devslab-kit-tenant-core"))

    // Bundle springdoc so a consumer gets Swagger UI just by adding the starter —
    // OpenApiAutoConfiguration (in -autoconfigure, @ConditionalOnClass(GroupedOpenApi))
    // then activates and serves /swagger-ui + /v3/api-docs with the admin API grouped.
    // Turn it off with devslab.kit.openapi.enabled=false (the bean stays dormant), or
    // exclude this dependency if you don't want the jar at all. Version pinned in
    // gradle.properties (the Spring Boot BOM doesn't manage springdoc).
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("SPRINGDOC_VERSION")}")
}

mavenPublishing {
    coordinates(
        providers.gradleProperty("GROUP").get(),
        "devslab-kit-spring-boot-starter",
        providers.gradleProperty("VERSION").get()
    )
    pom {
        name.set("devslab-kit-spring-boot-starter")
        description.set("devslab-kit :: Spring Boot starter (dependency bundle)")
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
