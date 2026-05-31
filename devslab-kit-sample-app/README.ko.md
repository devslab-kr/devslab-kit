# devslab-kit-sample-app

[English](README.md)

`devslab-kit-spring-boot-starter`를 사용하는 작은 Spring Boot 애플리케이션입니다.
**Maven Central에 배포하지 않습니다** — 의존 대상이 아니라 라이브러리를 떠받치기
위해 존재합니다.

## 이 모듈이 존재하는 이유

### 1. 통합 테스트 하니스 (이 repo 안에 두는 핵심 이유)

스타터가 **소비자가 실제로 쓰는 방식 그대로** 동작하는지 — 단지 컴파일되는지가
아니라 — 증명하는 자리입니다. **실제 PostgreSQL과 Redis**(Testcontainers)를 상대로
**전체 자동 구성**을 부팅해 플랫폼이 통째로 배선되는지 검증합니다:

- `SampleApplicationTests`는 컨텍스트를 띄우고 모든 스타터 빈이 존재하고 쓸 수
  있는지 확인합니다 — `TenantResolver`, `TenantContextHolder`,
  `CurrentUserProvider`, `PasswordHasher`, `LocalLoginService`,
  `PermissionChecker`, `MenuProvider`, `AuditEventPublisher` — 그리고 BCrypt 왕복.
- `BootstrapStatusEndpointTests`는 최초 관리자 부트스트랩(ADR 0001)과 관리자 REST
  API 응답을 검증합니다.
- `TestcontainersConfiguration`은 Postgres + Redis를 띄워 `@ServiceConnection`으로
  연결합니다.

단위 테스트로는 이 부류의 문제를 잡을 수 없습니다 — 자동 구성 순서, Spring Boot
BOM, 실제 방언 위의 JPA/Flyway, JSONB 바인딩, Redis JSON 직렬화는 전체가 실제
인프라를 상대로 부팅될 때만 깨집니다. 실제 버그 여럿(예: Redis 캐시 직렬화기, JWT
시계 처리)이 여기서 먼저 드러났습니다. **CI가 이 모듈에 의존**하므로 repo 안에
남습니다.

### 2. 살아있는 레퍼런스 설정

[`src/main/resources/application.yaml`](src/main/resources/application.yaml)은
스타터 설정의 완전히 동작하는 예시입니다 — datasource, Redis, 그리고
`devslab.kit.*` 설정(테넌트 모드/리졸버, 캐시 타입, 감사, 최초 관리자 부트스트랩).
실제 앱의 출발점으로 복사해 쓰세요.

### 3. 로컬 개발 플레이그라운드 & GraalVM 네이티브 타깃

실행해서 관리자 REST API를 둘러볼 수 있고
([devslab-kit-admin-ui](https://github.com/devslab-kr/devslab-kit-admin-ui)를
연결해 로그인), `nativeCompile`을 end-to-end로 검증하는 모듈이기도 합니다.

## 실행

### 테스트 (Docker 외 별도 설정 불필요)

```bash
./gradlew :devslab-kit-sample-app:test
```

Testcontainers가 일회용 Postgres + Redis를 자동으로 띄웁니다. 실행 중인 Docker만
있으면 됩니다.

### bootRun (Docker만 있으면 됨 — compose 자동)

이 모듈은 [`compose.yaml`](compose.yaml)(Postgres + Redis)을 포함하고
`spring-boot-docker-compose`에 의존하므로, `bootRun`이 그 컨테이너를 띄우고 연결까지
자동으로 배선합니다. 실행 중인 Docker만 있으면 됩니다:

```bash
./gradlew :devslab-kit-sample-app:bootRun
```

`application.yaml`의 `DEVSLAB_*` 기본값(localhost Postgres/Redis)은 대신 *외부*
데이터 저장소를 가리킬 때의 폴백입니다 — 예: CI/스테이징용
`DEVSLAB_DATASOURCE_URL`, `DEVSLAB_REDIS_HOST`, `DEVSLAB_CACHE_TYPE`.
부트스트랩은 로컬 편의를 위해 비밀번호 강제 변경을 끈 `admin`/`admin`을 시드합니다 —
운영에서는 이 형태를 쓰지 마세요.

## Maven Central에 없음 — 의도된 것

이 모듈은 배포에서 제외됩니다 (루트 `build.gradle.kts`의 `nonPublishedModules`).
테스트/레퍼런스 하니스이며, 라이브러리 모듈만 배포됩니다. 사용자용 데모가 필요하면
여기가 아니라 [devslab-examples](https://github.com/devslab-kr)에 둡니다.
