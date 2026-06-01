# Installation

`devslab-kit` publishes to Maven Central from `0.1.0`. The starter pulls in the
whole platform; depend on individual modules only if you want à la carte.

## Requirements

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| Datastore | PostgreSQL (primary; Flyway-migrated) |
| Cache | Redis (optional — only when `cache.type = redis`) |

## Add the dependency

=== "Gradle (Kotlin DSL)"

    ```kotlin
    implementation("kr.devslab:devslab-kit-spring-boot-starter:0.3.0")
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'kr.devslab:devslab-kit-spring-boot-starter:0.3.0'
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>kr.devslab</groupId>
      <artifactId>devslab-kit-spring-boot-starter</artifactId>
      <version>0.3.0</version>
    </dependency>
    ```

## À la carte

Each capability is a thin `-api` contract plus a `-core` default. Depend on a
`-core` for the batteries-included implementation, or on an `-api` alone to plug in
your own — the auto-configuration backs off (`@ConditionalOnMissingBean`) when you
do.

```kotlin
implementation("kr.devslab:devslab-kit-access-core:0.3.0")   // RBAC + groups + ABAC
implementation("kr.devslab:devslab-kit-cache-core:0.3.0")    // pluggable cache
// …or just the contract:
implementation("kr.devslab:devslab-kit-access-api:0.3.0")
```

See [Quick Start](quick-start.md) to boot a working app.

!!! note "Pre-1.0"
    The API may still change before `1.0`. To try unreleased changes (e.g. `main`),
    build from source: `./gradlew publishToMavenLocal` installs every module to your
    local `~/.m2`.
