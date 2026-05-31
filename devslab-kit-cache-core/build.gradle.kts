description = "devslab-kit :: cache default implementation"

dependencies {
    api(project(":devslab-kit-cache-api"))

    // Spring's cache abstraction (CacheManager, @EnableCaching). Always needed.
    api("org.springframework:spring-context")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Redis is OPTIONAL. The Redis-backed CacheManager (PR 2) only wires up when
    // the consumer puts these on the classpath themselves (type=redis). Keeping
    // them compileOnly here means the kit never forces Redis as a transitive
    // dependency — the in-memory default stays zero-dependency. See ADR 0002 §4.
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
