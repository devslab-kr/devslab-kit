description = "devslab-kit :: tenant default implementation"

dependencies {
    api(project(":devslab-kit-tenant-api"))

    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
