# 설정

모든 설정은 **`devslab.kit.*`** 접두사 아래에 있습니다. 런타임의 실제 적용값은
`GET /admin/api/v1/settings`에서 볼 수 있습니다(시크릿 마스킹).

!!! info "값 형식"
    - **Duration**은 ISO-8601: `PT8H` = 8시간, `PT15M` = 15분, `PT10M` = 10분,
      `PT30S` = 30초, `P1D` = 1일.
    - **Boolean**은 `true` / `false`.
    - enum 형태의 문자열 옵션은 아래에 허용 값을 나열합니다. 그 외 값은 시작 시 거부됩니다.

## Tenant — `devslab.kit.tenant.*` { #tenant }

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | 테넌트 계층 on/off. |
| `mode` | enum | `single` | 아래 **mode** 참고. |
| `default-tenant-id` | string | `default` | `single` 모드에서 쓰는 테넌트 id, 그리고 리졸버가 못 찾았을 때의 폴백. |
| `resolver` | enum | `fixed` | 활성 테넌트 결정 방식 — 아래 **resolver** 참고. |
| `header` | string | `X-Tenant-Id` | `header` 리졸버가 읽는 요청 헤더. |

**`mode`**

- `single` — 앱 전체가 테넌트 하나(항상 `default-tenant-id`). 추상화는 그대로 있어서
  코드는 멀티테넌트와 동일합니다.
- `multi` — 요청마다 `resolver`가 테넌트를 결정.

**`resolver`**

- `fixed` — 항상 `default-tenant-id` 반환. `single` 모드의 자연스러운 선택.
- `header` — 요청 헤더(`header` 속성, 기본 `X-Tenant-Id`)에서 테넌트 id를 읽음.
- `jwt` — 인증된 JWT의 클레임에서 테넌트를 읽음.
- `subdomain` — 요청 호스트의 서브도메인에서 유도(`acme.example.com` → `acme`).

[멀티테넌시 가이드](../guides/tenancy.md) 참고.

## Identity — `devslab.kit.identity.*` { #identity }

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `jwt.secret` | string | — | HMAC-SHA256 서명 키. **32바이트 이상**이어야 하고 운영에서 반드시 설정. 기본값 없음 — 직접 제공. |
| `jwt.issuer` | string | `devslab-kit` | JWT `iss` 클레임 값; 파싱 시 일치하지 않으면 토큰 거부. |
| `jwt.ttl` | duration | `PT8H` | 발급된 액세스 토큰의 유효 시간. |
| `max-failed-attempts` | int | `5` | 계정이 잠기기까지 허용하는 연속 로그인 실패 횟수. |
| `lockout-duration` | duration | `PT15M` | 임계치 도달 후 계정이 잠긴 채 유지되는 시간. |

[접근 제어 가이드](../guides/access.md) 참고.

## Audit — `devslab.kit.audit.*` { #audit }

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | 감사 로깅 on/off. |
| `async-queue-capacity` | int | `1024` | 비동기 라이터에 공급하는 bounded 큐 용량. 가득 차면 새 감사 이벤트를 요청을 막거나 메모리를 소진하는 대신 버립니다 — 피크 쓰기율에 맞게 크기 지정. |

[감사 가이드](../guides/audit.md) 참고.

## Menu — `devslab.kit.menu.*`

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | 동적 메뉴 모듈 on/off. |

[메뉴 가이드](../guides/menus.md) 참고.

## Cache — `devslab.kit.cache.*` { #cache }

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `type` | enum | `in-memory` | 캐시 백엔드 — 아래 **type** 참고. |
| `ttl` | duration | `PT10M` | 기본 엔트리 TTL. **`redis`만 적용**; `in-memory`는 무시(evict되거나 재시작 전까지 유지). |
| `key-prefix` | string | `devslab:` | Redis 키 앞에 붙는 네임스페이스 — 여러 앱이 한 Redis를 충돌 없이 공유. `in-memory`는 무시. |
| `cache-null-values` | boolean | `false` | `false`면 `null`을 반환하는 메서드는 캐시 안 됨(다음 호출이 다시 실행). 반복 miss 방어가 필요하면 `true`. |
| `allowed-package` | string | `kr.devslab` | Redis JSON 직렬화기가 다형 타이핑에 신뢰하는 패키지(`java.*`에 더해). 방어를 위해 자신의 베이스 패키지로 좁히세요. |

**`type`**

- `in-memory` — `ConcurrentMapCacheManager`. 단일 노드, TTL 없음. 기본값이며 로컬
  개발과 단일 인스턴스 앱에 적합.
- `redis` — Spring Data Redis + kit JSON 직렬화기. 엔트리가 replica 간 공유·일관되며
  `ttl`/`key-prefix`를 적용. `spring.data.redis.*` 필요.
- `none` — `NoOpCacheManager`; 캐시 비활성, 모든 조회가 재계산.

[캐시 가이드](../guides/cache.md), [ADR 0002](../adr/0002-distributed-cache.md) 참고.

## 최초 관리자 부트스트랩 — `devslab.kit.bootstrap.*`

빈 데이터베이스에 최초 관리자를 프로비저닝(ADR 0001). **기본 OFF**이며 명시적으로 켭니다.

| 속성 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | boolean | `false` | 시작 시 부트스트랩 실행. |
| `tenant-id` | string | `default` | 관리자(및 역할/권한)를 생성할 테넌트. |
| `admin-login-id` | string | `admin` | 시드되는 관리자의 로그인 id. |
| `admin-password` | string | — | 관리자 비밀번호. **비우면** 강력한 랜덤 비밀번호를 생성해 시작 시 **한 번** 로깅. |
| `admin-email` | string | — | 시드 관리자의 선택적 이메일. |
| `must-change-password` | boolean | `true` | 첫 로그인 시 새 비밀번호 설정 강제. |

!!! warning "운영 환경"
    `identity.jwt.secret`은 항상 강력하게 설정하세요. 부트스트랩은 강력한
    `admin-password` + `must-change-password: true`로 두거나, 비워서(랜덤, 한 번 로깅)
    두거나, 끄고 최초 관리자를 out-of-band로 프로비저닝하세요. `prod` / `production`
    프로파일에서는 kit이 **약한 부트스트랩 비밀번호를 거부**합니다.

[최초 관리자 부트스트랩 가이드](../guides/bootstrap.md),
[ADR 0001](../adr/0001-bootstrap-admin.md) 참고.

## OpenAPI / Swagger UI — `devslab.kit.openapi.*` { #openapi }

스타터가 **springdoc을 포함**하므로, 추가 의존성·설정 없이 스타터만으로
`/swagger-ui.html`과 `/v3/api-docs`가 올라오고, 킷의 `/admin/api/v1/**` 엔드포인트가
하나의 그룹(`/v3/api-docs/admin`)으로 묶입니다. springdoc `3.0.x`가 Spring Boot 4
라인입니다(`2.8.x`는 Spring Boot 3 대상).

끄는 방법 두 가지:

1. **jar는 두고 표면만 끔** — `devslab.kit.openapi.enabled=false`. 자동 구성이
   비활성 상태로 남아 아무것도 노출되지 않습니다. 프로덕션의 일반적 선택.
2. **jar 자체를 제거** — 스타터 의존성에서 springdoc을 exclude:

    === "Gradle (Kotlin DSL)"

        ```kotlin
        implementation("kr.devslab:devslab-kit-spring-boot-starter:0.2.1") {
            exclude(group = "org.springdoc")
        }
        ```

    === "Maven"

        ```xml
        <dependency>
          <groupId>kr.devslab</groupId>
          <artifactId>devslab-kit-spring-boot-starter</artifactId>
          <version>0.2.1</version>
          <exclusions>
            <exclusion>
              <groupId>org.springdoc</groupId>
              <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            </exclusion>
          </exclusions>
        </dependency>
        ```

| 프로퍼티 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | 마스터 스위치. `false`로 두면 (포함된) 의존성을 그대로 둔 채 킷의 OpenAPI 구성만 끕니다. |
| `admin-group` | string | `admin` | 관리자 API의 Swagger UI 그룹 이름. |
| `title` | string | `devslab-kit Admin API` | OpenAPI 문서 / Swagger UI에 표시될 제목. |
| `version` | string | `v1` | OpenAPI 문서의 버전 문자열. |

`OpenAPI` 문서 빈과 관리자 `GroupedOpenApi` 모두 `@ConditionalOnMissingBean`이므로,
직접 선언하면(예: security scheme이나 server 추가) 킷이 물러납니다.

!!! tip "프로덕션"
    API 문서는 보통 프로덕션에 노출하지 않습니다.
    `devslab.kit.openapi.enabled=false`로 표면을 끄거나(위 1번), springdoc을
    `exclude`해 jar를 제거하세요(2번).
