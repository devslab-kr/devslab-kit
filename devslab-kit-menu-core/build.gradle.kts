description = "devslab-kit :: menu default implementation"

dependencies {
    api(project(":devslab-kit-menu-api"))

    api("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
