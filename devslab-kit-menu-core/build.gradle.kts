description = "devslab-kit :: menu default implementation"

dependencies {
    api(project(":devslab-kit-menu-api"))
    // CacheNames (kit cache-name constants) + Spring's cache abstraction so the
    // menu cache rides the shared CacheManager (ADR 0002 §5) instead of its own
    // map — distributed automatically when the consumer sets cache.type=redis.
    api(project(":devslab-kit-cache-api"))
    api("org.springframework:spring-context")

    api("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
