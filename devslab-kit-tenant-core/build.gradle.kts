description = "devslab-kit :: tenant default implementation"

dependencies {
    api(project(":devslab-kit-tenant-api"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
