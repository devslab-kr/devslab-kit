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
    api(project(":devslab-kit-admin-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Micrometer for custom metrics; consumer's spring-boot-actuator activates it at runtime.
    compileOnly("io.micrometer:micrometer-core")
}
