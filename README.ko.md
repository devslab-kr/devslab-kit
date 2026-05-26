# devslab-kit

[English README](README.md)

DevsLab 제품군에서 재사용하는 플랫폼 Spring Boot Starter.

`devslab-kit`은 인증, 계정, 권한, 역할, 멀티/싱글 테넌트, 메뉴, 감사 로그, Admin API,
Spring Boot AutoConfiguration 같은 **여러 제품이 공통으로 쓰는 플랫폼 빌딩 블록**을 제공한다.
각 제품은 자기 도메인에만 집중할 수 있게 된다.

이 kit은 **특정 제품 도메인을 절대 알지 않는다.** 첫 번째 실사용 제품은 `bookrecord`지만,
`devslab-kit` 자체는 제품에 독립적이어야 한다.

## 상태

> **`0.1.0` 이전 부트스트랩 단계.** 현재 리포지토리에는 IntelliJ New Project 마법사로 생성한
> Spring Boot 4 초기 프로젝트와, 조직 컨벤션(라이선스, 변경 이력, 양 언어 README)만 들어 있다.
> 멀티모듈 분리, 공개 계약, AutoConfiguration, 샘플 앱은 [`CHANGELOG.md`](CHANGELOG.md)에 따라
> 후속 PR에서 들어온다.

## 기술 스택

| 계층             | 선택                                                       |
| ---------------- | ---------------------------------------------------------- |
| 언어             | Java 25                                                    |
| 프레임워크       | Spring Boot 4.x                                            |
| 빌드             | Gradle (Kotlin DSL)                                        |
| Group / Package  | `kr.devslab` / `kr.devslab.kit`                            |
| 웹 스택          | Spring Web MVC (Servlet) — WebFlux는 core에 **없음**       |
| 보안             | Spring Security (Servlet)                                  |
| 영속성           | Spring Data JPA + Flyway + PostgreSQL                      |
| 캐시 / 세션      | Spring Data Redis (Spring Session은 아직 core에 **없음**)  |
| 관측             | Spring Boot Actuator                                       |
| 네이티브         | GraalVM Native Build Tools                                 |
| 로컬 개발 / 테스트 | Docker Compose + Testcontainers (PostgreSQL, Redis)        |

## 설계 원칙

1. **제품에 독립적.** `bookrecord`(또는 다른 어떤 제품)의 도메인 타입도 `devslab-kit`에
   들어오지 않는다. `UserId`, `TenantId`, `Permission`, `Role`, `Menu`, `Audit` 같은
   플랫폼 개념만 둔다.
2. **계약은 Java API다.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring Session은 모두
   **선택형 starter**이며 절대 core에 넣지 않는다.
3. **AutoConfiguration은 override 친화적.** 모든 기본 Bean은 `@ConditionalOnMissingBean`이라
   소비 앱이 kit을 fork하지 않고도 교체할 수 있다.
4. **TenantContext는 항상 존재한다** — 싱글 테넌트라도 default tenant를 resolve하지,
   추상화를 건너뛰지 않는다.
5. **권한은 메뉴를 모른다.** 메뉴는 권한을 참조할 수 있지만, 그 반대 방향 의존은 없다.
6. **인증 계정 ≠ 서비스 프로필.** `platform_user_account`는 로그인 / 상태 / 테넌시만 갖는다.
   닉네임, 아바타, 취향 같은 제품별 프로필 데이터는 제품 테이블에 둔다.
7. **GraalVM Native 친화적.** 리플렉션 중심 설계를 피하고, 샘플 앱이 `nativeCompile`을
   end-to-end로 검증한다.

## 예정 모듈 구조

```text
devslab-kit/
├─ devslab-kit-bom
├─ devslab-kit-core
├─ devslab-kit-identity-{api,core}
├─ devslab-kit-access-{api,core}
├─ devslab-kit-tenant-{api,core}
├─ devslab-kit-menu-{api,core}
├─ devslab-kit-audit-{api,core}
├─ devslab-kit-autoconfigure
├─ devslab-kit-spring-boot-starter
├─ devslab-kit-admin-{api,ui,starter}
├─ devslab-kit-test-support
└─ devslab-kit-sample-app
```

선택형 adapter starter (필요할 때 추가, 기본 starter에는 들어가지 않음):

```text
devslab-kit-graphql-dgs-starter
devslab-kit-webflux-starter
devslab-kit-rabbitmq-starter
devslab-kit-spring-session-starter
devslab-kit-oauth2-{client,resource-server}-starter
```

## 로컬 실행

```bash
./gradlew test
./gradlew bootRun
```

Docker Compose(`compose.yaml`)는 Spring Boot의 `spring-boot-docker-compose` 통합으로
`bootRun` 시에 PostgreSQL과 Redis를 자동 기동한다. 테스트는 `@ServiceConnection` 기반
Testcontainers로 동작한다.

## 라이선스

[Apache License 2.0](LICENSE)
