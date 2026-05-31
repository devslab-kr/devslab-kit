description = "devslab-kit :: cache default implementation"

dependencies {
    api(project(":devslab-kit-cache-api"))

    // Spring's cache abstraction (CacheManager, @EnableCaching). Always needed.
    api("org.springframework:spring-context")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Redis is OPTIONAL. The Redis-backed CacheManager only wires up when the
    // consumer puts these on the classpath themselves (type=redis). Keeping them
    // compileOnly here means the kit never forces Redis as a transitive
    // dependency — the in-memory default stays zero-dependency. See ADR 0002 §4.
    // (spring-boot-starter-data-redis brings Spring Data Redis + Jackson 3
    //  tools.jackson transitively, which GenericJacksonJsonRedisSerializer needs.)
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // The Redis path is exercised against a real Redis (Testcontainers): the
    // serialization round-trip is the whole risk, so it gets an integration test
    // rather than a mock. Redis deps are test-scoped here (compileOnly above for
    // main), matching how a consumer who opts into type=redis would add them.
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}
