# devslab-kit

[English README](README.md) · [변경 이력](CHANGELOG.ko.md)

재사용 가능한 **Spring Boot 4 플랫폼 스타터**. 애플리케이션에 끼워 넣으면 인증, 인가,
멀티테넌시, 동적 메뉴, 감사 로깅이 자동 구성으로 제공되고, 관리자 REST API와 바로 쓰는
관리자 콘솔까지 딸려옵니다. 각 제품은 자기 도메인에만 집중할 수 있습니다.

`devslab-kit`은 의도적으로 **제품에 독립적**입니다. `UserId`, `TenantId`, `Permission`,
`Role`, `Menu`, `Audit` 같은 플랫폼 개념만 알 뿐, 특정 제품의 도메인은 절대 알지 않습니다.

> **상태 — 1.0 이전.** 첫 공개 릴리스 `0.1.0`에 필요한 기능은 모두 완성되었습니다.
> `0.1.0`부터 Maven Central에 아티팩트를 배포합니다.

## 핵심 기능

- **Identity** — 사용자, 자격 증명, JWT 발급/파싱, 설정 가능한 로그인 잠금.
- **Access** — 역할, 권한, 주체 **그룹**, 그리고 RBAC 위에 얹은 **ABAC** 정책 SPI
  (`PolicyEvaluator`).
- **멀티테넌시** — *항상 존재하는* 테넌트 컨텍스트 (싱글 테넌트라도 추상화를 건너뛰지 않고
  default를 resolve), 플러그형 리졸버: `fixed`, `header`, `jwt`, `subdomain`.
- **메뉴** — 사용자별 권한 필터링 동적 메뉴 트리.
- **감사(Audit)** — `ApplicationEventPublisher` 기반 비동기 감사 로깅.
- **캐시** — 플러그형 캐시 (`devslab.kit.cache.type` = `in-memory` / `redis` / `none`);
  Redis 백엔드가 JSON 직렬화를 직접 책임지므로 `Serializable`이나 직렬화기 설정을 만질 일이
  없습니다 (ADR 0002).
- **최초 관리자 부트스트랩** — 첫 부팅 시 테넌트, `PLATFORM_ADMIN` 역할, `admin.*` 권한,
  관리자 사용자를 생성합니다. 비밀번호 강제 변경 옵션 포함 (ADR 0001).
- **관리자 REST API** — 사용자, 역할, 권한, 그룹, 메뉴, 테넌트, 정책, 감사 로그, 진단, 설정을
  위한 `/admin/api/v1/**`.
- **Override 친화적** — 모든 기본 빈이 `@ConditionalOnMissingBean`이라, kit을 fork하지 않고도
  어느 부분이든 교체할 수 있습니다.

## 동반 관리자 콘솔

[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui)는
관리자 REST API 위에 바로 올린 Vue 3 + PrimeVue 콘솔입니다 — 로그인, 사용자, 역할, 권한,
그룹, 메뉴, 테넌트, ABAC 정책 테스트, 감사 로그 검색, 진단, 실시간 설정 뷰. 그대로 쓰거나
직접 UI를 만들 때 참고용으로 쓰세요.

## 설치

> `0.1.0`부터 Maven Central에서 받을 수 있습니다.

스타터가 플랫폼 전체를 끌어옵니다:

**Gradle (Kotlin DSL)**

```kotlin
implementation("kr.devslab:devslab-kit-spring-boot-starter:0.1.0")
```

**Maven**

```xml
<dependency>
  <groupId>kr.devslab</groupId>
  <artifactId>devslab-kit-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

전체 스타터가 필요 없으면 개별 모듈(예: `devslab-kit-access-core`)에만 의존할 수도 있습니다.

### 요구 사항

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| 데이터 저장소 | PostgreSQL (주 저장소) |
| 캐시 | Redis (선택 — 분산 캐시에만 필요) |

## 빠른 시작

1. 스타터를 추가합니다 (위 참조).
2. 데이터베이스와 (선택적으로) Redis를 가리킵니다:

   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/app
       username: app
       password: app
     data:
       redis:
         host: localhost      # cache.type = redis 일 때만 필요

   devslab:
     kit:
       tenant:
         mode: single         # 또는 multi
         resolver: fixed      # fixed | header | jwt | subdomain
       cache:
         type: in-memory      # in-memory | redis | none
       bootstrap:
         enabled: true        # 첫 부팅 시 최초 관리자 생성
   ```

3. 앱을 부팅합니다. 부트스트랩이 `PLATFORM_ADMIN`을 시드하고, 관리자 REST API가
   `/admin/api/v1/**`에서 동작합니다.
   [devslab-kit-admin-ui](https://github.com/devslab-kr/devslab-kit-admin-ui)를
   여기에 연결해 로그인하세요.

Docker Compose(Postgres + Redis)와 Testcontainers까지 갖춘 완전히 동작하는 설정은
[`devslab-kit-sample-app`](devslab-kit-sample-app)을 참고하세요.

## 모듈

| 모듈 | 역할 |
| --- | --- |
| `devslab-kit-core` | 공유 값 객체 (`TenantId`, `UserId`, `PublicId`, …) |
| `devslab-kit-tenant-{api,core}` | 테넌트 컨텍스트 + 리졸버 (fixed/header/jwt/subdomain) |
| `devslab-kit-identity-{api,core}` | 사용자, 자격 증명, JWT, 로그인 잠금 |
| `devslab-kit-access-{api,core}` | 역할, 권한, 그룹, ABAC 정책 엔진 |
| `devslab-kit-menu-{api,core}` | 권한 필터링 동적 메뉴 |
| `devslab-kit-audit-{api,core}` | 비동기 감사 로깅 |
| `devslab-kit-cache-{api,core}` | 플러그형 캐시 (in-memory / Redis) |
| `devslab-kit-admin-api` | 관리자 REST 엔드포인트 (`/admin/api/v1/**`) |
| `devslab-kit-autoconfigure` | Spring Boot 자동 구성 |
| `devslab-kit-spring-boot-starter` | 스타터 — 플랫폼 전체를 끌어옴 |
| `devslab-kit-sample-app` | 실행 가능한 참조 앱 (배포 안 함) |

`*-api` 모듈은 공개 계약을, `*-core` 모듈은 기본 구현을 담습니다. `-api`에만 의존해 직접
구현할 수도 있습니다.

## 설계 원칙

1. **제품에 독립적.** 제품 도메인 타입은 들어오지 않고, 플랫폼 개념만 둡니다.
2. **계약은 Java API.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring Session은 선택형
   추가 기능이며 절대 core가 아닙니다.
3. **Override 친화적 자동 구성.** 모든 기본 빈은 `@ConditionalOnMissingBean`.
4. **TenantContext는 항상 존재** — 싱글 테넌트라도 default를 resolve합니다.
5. **권한은 메뉴를 모름.** 메뉴는 권한을 참조할 수 있지만 그 반대 의존은 없습니다.
6. **인증 계정 ≠ 서비스 프로필.** 플랫폼 계정은 로그인 / 상태 / 테넌시만 갖고, 제품별
   프로필 데이터는 제품 테이블에 둡니다.
7. **GraalVM Native 친화적.** 리플렉션 중심 설계를 피합니다.

## 문서

- 아키텍처 결정 기록: [`docs/adr`](docs/adr) — ADR 0001(최초 관리자 부트스트랩),
  ADR 0002(플러그형 캐시). 둘 다 한/영 양 언어.
- 변경 이력: [`CHANGELOG.ko.md`](CHANGELOG.ko.md) ([English](CHANGELOG.md)).

## 로컬 실행

```bash
./gradlew build              # 컴파일 + 테스트 (Testcontainers: Postgres + Redis)
./gradlew :devslab-kit-sample-app:bootRun
```

## 라이선스

[Apache License 2.0](LICENSE)
