description = "devslab-kit :: admin REST API"

dependencies {
    api(project(":devslab-kit-identity-api"))
    api(project(":devslab-kit-identity-core"))
    api(project(":devslab-kit-access-api"))
    api(project(":devslab-kit-access-core"))
    api(project(":devslab-kit-menu-api"))
    api(project(":devslab-kit-menu-core"))
    api(project(":devslab-kit-audit-api"))
    api(project(":devslab-kit-audit-core"))
    api(project(":devslab-kit-tenant-api"))

    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
