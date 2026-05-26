description = "devslab-kit :: audit log default implementation"

dependencies {
    api(project(":devslab-kit-audit-api"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.fasterxml.jackson.core:jackson-databind")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
