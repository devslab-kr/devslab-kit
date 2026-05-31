plugins {
    id("org.springframework.boot")
    id("org.graalvm.buildtools.native")
}

description = "devslab-kit :: sample application (starter verification)"

dependencies {
    implementation(project(":devslab-kit-spring-boot-starter"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

// Pin the main class explicitly. Auto-detection (ResolveMainClassName) does not
// resolve it in this multi-module setup where the `java` plugin is applied to
// subprojects from the root after the Spring Boot plugin, leaving main-class
// resolution — and therefore `./gradlew build` — to fail. The GraalVM native
// plugin also pulls in the `application` plugin, whose `startScripts` task reads
// `application.mainClass`. Setting it here feeds bootJar/bootRun (via
// convention), startScripts, and nativeCompile.
application {
    mainClass.set("kr.devslab.kit.sample.SampleApplication")
}
