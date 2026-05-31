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
    // Our RedisCacheConfiguration references Jackson 3 (JsonMapper) directly to
    // build the value serializer. Spring Data Redis declares Jackson `optional`,
    // so it isn't on the compile classpath transitively — declare it explicitly.
    // compileOnly (like Redis itself): only needed when the consumer opts into
    // type=redis, which already puts Jackson 3 on their runtime classpath via
    // spring-boot-starter-data-redis. Version managed by the Spring Boot BOM.
    compileOnly("tools.jackson.core:jackson-databind")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // The Redis path is exercised against a real Redis (Testcontainers): the
    // serialization round-trip is the whole risk, so it gets an integration test
    // rather than a mock. Redis deps are test-scoped here (compileOnly above for
    // main), matching how a consumer who opts into type=redis would add them.
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    // Jackson 3 is `optional` in Spring Data Redis, so the Redis starter doesn't
    // pull it transitively even at test scope. The Redis value serializer needs
    // it at runtime (PolymorphicTypeValidator etc.) — declare it explicitly for
    // the test runtime, mirroring the compileOnly on main. A real consumer who
    // opts into type=redis gets Jackson 3 from spring-boot-starter-jackson, which
    // SB4 apps always have.
    testImplementation("tools.jackson.core:jackson-databind")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}
