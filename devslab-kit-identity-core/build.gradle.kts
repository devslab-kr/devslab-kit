description = "devslab-kit :: identity default implementation"

dependencies {
    api(project(":devslab-kit-identity-api"))
    api(project(":devslab-kit-tenant-api"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.security:spring-security-crypto")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
