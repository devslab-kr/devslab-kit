# devslab-kit

[![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/devslab-kit-spring-boot-starter?logo=apachemaven)](https://central.sonatype.com/artifact/kr.devslab/devslab-kit-spring-boot-starter)
[![Build](https://github.com/devslab-kr/devslab-kit/actions/workflows/build.yml/badge.svg)](https://github.com/devslab-kr/devslab-kit/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/devslab-kr/devslab-kit/branch/main/graph/badge.svg)](https://codecov.io/gh/devslab-kr/devslab-kit)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot)

[English README](README.md) · [변경 이력](CHANGELOG.ko.md) · [ADR](docs/adr)

재사용 가능한 **Spring Boot 4 플랫폼 스타터**. 애플리케이션에 끼워 넣으면 인증, 인가,
멀티테넌시, 동적 메뉴, 감사 로깅이 자동 구성으로 제공되고, 관리자 REST API와 바로 쓰는
관리자 콘솔까지 딸려옵니다. 매번 플랫폼 계층을 다시 만드는 대신 각 제품은 자기 도메인에만
집중할 수 있습니다.

`devslab-kit`은 의도적으로 **제품에 독립적**입니다. `UserId`, `TenantId`, `Permission`,
`Role`, `Menu`, `Audit` 같은 플랫폼 개념만 알 뿐, 특정 제품의 도메인은 절대 알지 않습니다.

> **상태 — 1.0 이전.** 첫 공개 릴리스 `0.1.0`에 필요한 기능은 모두 완성되었습니다.
> `0.1.0`부터 Maven Central에 배포하며, 그 전에는 소스 빌드나 `publishToMavenLocal`을
> 사용하세요.

## 목차

- [기능](#기능) · [요구-사항](#요구-사항) · [설치](#설치)
- [빠른-시작](#빠른-시작) · [설정](#설정)
- [모듈](#모듈) · [관리자-rest-api](#관리자-rest-api) · [관리자-콘솔](#관리자-콘솔)
- [설계-원칙](#설계-원칙) · [문서](#문서) · [소스에서-빌드](#소스에서-빌드)
- [버전-정책](#버전-정책) · [라이선스](#라이선스)

## 기능

| 영역 | 제공 내용 |
| --- | --- |
| **Identity** | 사용자 계정, BCrypt 자격 증명, JWT 발급/파싱, 설정 가능한 로그인 잠금, 비밀번호 강제 변경. |
| **Access** | 역할, 권한, 주체 **그룹**, 그리고 RBAC 위에 얹은 **ABAC** 정책 SPI(`PolicyEvaluator`). |
| **멀티테넌시** | *항상 존재하는* 테넌트 컨텍스트(싱글 테넌트라도 추상화를 건너뛰지 않고 default를 resolve), 플러그형 리졸버: `fixed` · `header` · `jwt` · `subdomain`; `single`/`multi` 모드. |
| **메뉴** | 사용자별로 계산되는 권한 필터링 동적 메뉴 트리. |
| **감사(Audit)** | `ApplicationEventPublisher` 기반 비동기 감사 로깅, PostgreSQL(JSONB 메타데이터)에 영속화. |
| **캐시** | 플러그형 캐시 — `in-memory` · `redis` · `none`. Redis 백엔드가 JSON 직렬화를 직접 책임지므로 `Serializable` 구현이나 직렬화기 배선이 필요 없습니다(ADR 0002). 사용자별 메뉴 캐시도 이 공유 매니저를 사용합니다. |
| **최초 관리자 부트스트랩** | 첫 부팅 시 테넌트, `PLATFORM_ADMIN` 역할, `admin.*` 권한, 관리자 사용자를 멱등하게 생성 — opt-in, 프로퍼티 기반(ADR 0001). |
| **관리자 REST API** | 위 모든 엔티티 + 진단 + 실시간 설정 뷰를 위한 `/admin/api/v1/**`. |
| **OpenAPI / Swagger UI** | 스타터에 포함 — `/swagger-ui`가 관리자 API 그룹과 함께 자동으로 뜸, 설정 불필요. `openapi.enabled=false`로 끄거나, springdoc 의존성을 `exclude`해 jar 자체를 제거. |
| **Override 친화적** | 모든 기본 빈이 `@ConditionalOnMissingBean` — 직접 선언하면 어느 조각이든 교체 가능. |
| **GraalVM Native** | 리플렉션 중심 설계를 피하고, 샘플 앱이 `nativeCompile`을 검증. |

## 요구 사항

| | |
| --- | --- |
| Java | 21+ |
| Spring Boot | 4.0+ |
| 데이터 저장소 | PostgreSQL (주 저장소; Flyway 마이그레이션) |
| 캐시 | Redis (선택 — `cache.type = redis`일 때만) |
| 웹 스택 | Spring Web MVC (Servlet) + Spring Security |

## 설치

> `0.1.0`부터 Maven Central에서 받을 수 있습니다. 스타터가 플랫폼 전체를 끌어옵니다.

**Gradle (Kotlin DSL)**

```kotlin
implementation("kr.devslab:devslab-kit-spring-boot-starter:0.2.1")
```

**Maven**

```xml
<dependency>
  <groupId>kr.devslab</groupId>
  <artifactId>devslab-kit-spring-boot-starter</artifactId>
  <version>0.2.1</version>
</dependency>
```

원하는 모듈만? 개별 모듈(예: `devslab-kit-access-core`)에만 의존하거나, `-api` 계약에만
의존해 직접 구현을 제공할 수도 있습니다.

## 빠른 시작

**1. 스타터 추가** (위 참조).

**2. 설정** — datasource와 플랫폼:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: app
    password: app
  data:
    redis:
      host: localhost          # cache.type = redis 일 때만 필요

devslab:
  kit:
    tenant:
      mode: single             # single | multi
      resolver: fixed          # fixed | header | jwt | subdomain
      default-tenant-id: default
    identity:
      jwt:
        secret: ${DEVSLAB_JWT_SECRET}   # HS256용 32바이트 이상 — 운영에서 설정
        ttl: PT8H
      max-failed-attempts: 5            # N회 실패 시 계정 잠금
      lockout-duration: PT15M
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # 첫 부팅 시 최초 관리자 생성
```

**3. 앱 부팅.** 부트스트랩이 `PLATFORM_ADMIN`을 시드하고, 관리자 REST API가
`/admin/api/v1/**`에서 동작하며, Flyway가 `platform_*` 테이블을 생성합니다.
[관리자 콘솔](#관리자-콘솔)을 연결해 로그인하세요.

Docker Compose(Postgres + Redis)와 Testcontainers 기반 테스트까지 갖춘 완전히
동작하는 설정은 [`devslab-kit-sample-app`](devslab-kit-sample-app)
([README](devslab-kit-sample-app/README.ko.md))에 있습니다.

## 설정

모든 키는 `devslab.kit.*` 접두사 아래에 있습니다. 기본값 표기.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `tenant.enabled` | `true` | 테넌트 계층 마스터 스위치. |
| `tenant.mode` | `single` | `single` 또는 `multi`. |
| `tenant.default-tenant-id` | `default` | single 모드/폴백에 쓰는 테넌트. |
| `tenant.resolver` | `fixed` | `fixed` · `header` · `jwt` · `subdomain`. |
| `identity.jwt.secret` | — | HS256용 32바이트 이상 키. **운영 필수.** |
| `identity.jwt.issuer` | `devslab-kit` | JWT `iss` 클레임. |
| `identity.jwt.ttl` | `PT8H` | 토큰 수명(ISO-8601 duration). |
| `identity.max-failed-attempts` | `5` | 이 횟수만큼 실패하면 계정 잠금. |
| `identity.lockout-duration` | `PT15M` | 계정 잠금 유지 시간. |
| `audit.enabled` | `true` | 감사 로깅 토글. |
| `audit.async-queue-capacity` | `1024` | 비동기 발행기의 bounded 큐. |
| `menu.enabled` | `true` | 동적 메뉴 토글. |
| `cache.type` | `in-memory` | `in-memory` · `redis` · `none`. |
| `cache.ttl` | `PT10M` | 엔트리 TTL(Redis 백엔드에서 사용). |
| `cache.key-prefix` | `devslab:` | Redis 키 네임스페이스. |
| `cache.allowed-package` | `kr.devslab` | 안전한 다형 JSON 타이핑 허용 목록. |
| `bootstrap.enabled` | `false` | 첫 부팅 시 최초 관리자 생성. |
| `bootstrap.admin-login-id` | `admin` | 최초 관리자 로그인 id. |
| `bootstrap.admin-password` | — | 비우면 강력한 랜덤 비밀번호를 한 번 로깅. |
| `bootstrap.must-change-password` | `true` | 첫 로그인 시 변경 강제. |
| `openapi.enabled` | `true` | Swagger UI / OpenAPI 노출(springdoc은 스타터에 포함됨). `false`로 비활성화(예: 프로덕션). |
| `openapi.title` | `devslab-kit Admin API` | OpenAPI 문서 / Swagger UI에 표시될 제목. |

런타임의 실제 적용값은 `GET /admin/api/v1/settings`에서도 볼 수 있습니다(시크릿 마스킹).

## 모듈

| 모듈 | 역할 |
| --- | --- |
| `devslab-kit-core` | 공유 값 객체 (`TenantId`, `UserId`, `PublicId`, …) |
| `devslab-kit-tenant-{api,core}` | 테넌트 컨텍스트 + 리졸버 |
| `devslab-kit-identity-{api,core}` | 사용자, 자격 증명, JWT, 로그인 잠금 |
| `devslab-kit-access-{api,core}` | 역할, 권한, 그룹, ABAC 정책 엔진 |
| `devslab-kit-menu-{api,core}` | 권한 필터링 동적 메뉴 |
| `devslab-kit-audit-{api,core}` | 비동기 감사 로깅 |
| `devslab-kit-cache-{api,core}` | 플러그형 캐시 (in-memory / Redis) |
| `devslab-kit-admin-api` | 관리자 REST 엔드포인트 |
| `devslab-kit-autoconfigure` | Spring Boot 자동 구성 |
| `devslab-kit-spring-boot-starter` | 스타터 — 플랫폼 전체를 끌어옴 |
| `devslab-kit-sample-app` | 실행 가능한 참조 앱 + 통합 테스트 하니스 (배포 안 함) |

**`-api` vs `-core`.** 각 기능은 얇은 계약 모듈(`-api`)과 기본 구현(`-core`)으로
나뉩니다. 배터리 포함 기본값을 쓰려면 `-core`에, 직접 구현을 끼우려면 `-api`에만
의존하세요 — 그러면 자동 구성이 물러납니다(`@ConditionalOnMissingBean`).

## 관리자 REST API

모두 `/admin/api/v1` 아래:

| 리소스 | 엔드포인트 |
| --- | --- |
| `auth` | 로그인, 비밀번호 변경 |
| `users` · `roles` · `permissions` · `groups` | 전체 CRUD + 할당 |
| `menus` · `tenants` | 메뉴 트리/테넌트 관리 |
| `policies` | ABAC 정책 목록 + `(subject, action, resource)` 드라이런 |
| `audit-logs` | 감사 추적 검색/필터 |
| `diagnostics` | 읽기 전용 로그인/권한/메뉴 가시성 프로브 |
| `settings` | 실시간 `devslab.kit.*` 뷰(시크릿 마스킹) |
| `bootstrap/status` | 최초 실행 흐름용 비인증 `{ initialized: boolean }` |

## 관리자 콘솔

[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui)는
이 REST API 위에 바로 올린 Vue 3 + PrimeVue 콘솔입니다 — 로그인, 모든 엔티티 화면,
ABAC 정책 테스트, 감사 로그 검색, 진단, 실시간 설정 뷰까지 모두 한/영 양 언어. 그대로
쓰거나 직접 UI를 만들 때 참고하세요.

## 설계 원칙

1. **제품에 독립적.** 제품 도메인 타입은 들어오지 않고, 플랫폼 개념만 둡니다.
2. **계약은 Java API.** GraphQL, WebFlux, RabbitMQ, OAuth2, Spring Session은 선택형 추가 기능이며 절대 core가 아닙니다.
3. **Override 친화적 자동 구성.** 모든 기본 빈은 `@ConditionalOnMissingBean`.
4. **TenantContext는 항상 존재** — 싱글 테넌트라도 default를 resolve합니다.
5. **권한은 메뉴를 모름.** 메뉴는 권한을 참조할 수 있지만 그 반대 의존은 없습니다.
6. **인증 계정 ≠ 서비스 프로필.** 플랫폼 계정은 로그인 / 상태 / 테넌시만 갖고, 제품별 프로필 데이터는 제품 테이블에 둡니다.
7. **GraalVM Native 친화적.** 리플렉션 중심 설계를 피합니다.

## 문서

- **아키텍처 결정 기록** — [`docs/adr`](docs/adr): ADR 0001(최초 관리자 부트스트랩),
  ADR 0002(플러그형 캐시). 한/영 양 언어.
- **변경 이력** — [`CHANGELOG.ko.md`](CHANGELOG.ko.md) ([English](CHANGELOG.md)).

## 소스에서 빌드

```bash
./gradlew build                              # 컴파일 + 테스트 (Testcontainers: Postgres + Redis; Docker 필요)
./gradlew publishToMavenLocal                # 모든 모듈을 ~/.m2 에 설치
./gradlew :devslab-kit-sample-app:bootRun    # 참조 앱 실행
```

Java 21(빌드는 GraalVM 21 toolchain 사용)과 통합 테스트용으로 실행 중인 Docker가
필요합니다.

## 버전 정책

라이브러리 메이저는 Spring Boot 메이저와 정렬됩니다: **`4.x.y`는 Spring Boot 4.x를
대상으로** 합니다. 릴리스는 [유의적 버전](https://semver.org/lang/ko/)을 따릅니다.
마이그레이션 노트는 [변경 이력](CHANGELOG.ko.md)을 참고하세요.

## 라이선스

[Apache License 2.0](LICENSE)
