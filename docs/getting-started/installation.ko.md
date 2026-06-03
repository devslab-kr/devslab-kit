# 설치

`devslab-kit`은 `0.1.0`부터 Maven Central에 배포됩니다. 스타터가 플랫폼 전체를
끌어오며, 원하는 모듈만 쓰고 싶을 때만 개별 모듈에 의존하세요.

## 요구 사항

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| 데이터 저장소 | PostgreSQL (주 저장소; Flyway 마이그레이션) |
| 캐시 | Redis (선택 — `cache.type = redis`일 때만) |

## 의존성 추가

=== "Gradle (Kotlin DSL)"

    ```kotlin
    implementation("kr.devslab:devslab-kit-spring-boot-starter:0.5.0")
    ```

=== "Gradle (Groovy)"

    ```groovy
    implementation 'kr.devslab:devslab-kit-spring-boot-starter:0.5.0'
    ```

=== "Maven"

    ```xml
    <dependency>
      <groupId>kr.devslab</groupId>
      <artifactId>devslab-kit-spring-boot-starter</artifactId>
      <version>0.5.0</version>
    </dependency>
    ```

## 모듈 선택 (à la carte)

각 기능은 얇은 `-api` 계약과 `-core` 기본 구현으로 나뉩니다. 배터리 포함 구현을 쓰려면
`-core`에, 직접 구현을 끼우려면 `-api`에만 의존하세요 — 그러면 자동 구성이
물러납니다(`@ConditionalOnMissingBean`).

```kotlin
implementation("kr.devslab:devslab-kit-access-core:0.5.0")   // RBAC + 그룹 + ABAC
implementation("kr.devslab:devslab-kit-cache-core:0.5.0")    // 플러그형 캐시
// …또는 계약만:
implementation("kr.devslab:devslab-kit-access-api:0.5.0")
```

동작하는 앱을 부팅하려면 [빠른 시작](quick-start.md)을 참고하세요.

!!! note "1.0 이전"
    `1.0` 전까지 API가 바뀔 수 있습니다. 미발행 변경(예: `main`)을 써보려면 소스에서
    빌드하세요: `./gradlew publishToMavenLocal`이 모든 모듈을 로컬 `~/.m2`에 설치합니다.
