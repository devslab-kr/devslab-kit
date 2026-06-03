# devslab-kit

재사용 가능한 **Spring Boot 4 플랫폼 스타터**. 애플리케이션에 끼워 넣으면 인증, 인가,
멀티테넌시, 동적 메뉴, 감사 로깅이 자동 구성으로 제공되고, 관리자 REST API와 바로 쓰는
관리자 콘솔까지 딸려옵니다. 매번 플랫폼 계층을 다시 만드는 대신 각 제품은 자기 도메인에만
집중할 수 있습니다.

`devslab-kit`은 의도적으로 **제품에 독립적**입니다. `UserId`, `TenantId`, `Permission`,
`Role`, `Menu`, `Audit` 같은 플랫폼 개념만 알 뿐, 특정 제품의 도메인은 절대 알지 않습니다.

[시작하기](getting-started/installation.md){ .md-button .md-button--primary }
[GitHub에서 보기](https://github.com/devslab-kr/devslab-kit){ .md-button }

!!! note "상태 — 1.0 이전"
    첫 공개 릴리스 `0.1.0`에 필요한 기능은 모두 완성되었습니다. `0.1.0`부터 Maven
    Central에 아티팩트를 배포합니다.

## 제공 내용

<div class="grid cards" markdown>

-   :material-account-key: **Identity**

    사용자, BCrypt 자격 증명, JWT 발급/파싱, 설정 가능한 로그인 잠금, 비밀번호 강제 변경.

-   :material-shield-lock: **Access**

    역할, 권한, 주체 **그룹**, 그리고 RBAC 위에 얹은 **ABAC** 정책 SPI.

-   :material-domain: **멀티테넌시**

    *항상 존재하는* 테넌트 컨텍스트와 플러그형 리졸버 — `fixed`, `header`, `jwt`,
    `subdomain`.

-   :material-menu: **동적 메뉴**

    사용자별로 계산되는 권한 필터링 메뉴 트리.

-   :material-clipboard-text-clock: **감사 로깅**

    `ApplicationEventPublisher` 기반 비동기 감사 추적, PostgreSQL에 영속화.

-   :material-database-arrow-right: **플러그형 캐시**

    `in-memory`, `redis`, `none`. Redis 백엔드가 JSON 직렬화를 직접 책임집니다 —
    `Serializable`도, 직렬화기 배선도 필요 없습니다.

-   :material-sync: **설정 동기화**

    권한·역할·메뉴를 환경 간에 코드 기준 export/import 번들로 승격 — `merge` 또는
    `mirror`, 먼저 dry-run ([가이드](guides/config-sync.ko.md)).

</div>

## 왜 스타터인가? { #why-a-starter }

팀이 만드는 모든 제품은 같은 플랫폼 계층을 필요로 합니다: 사용자가 누구인지, 무엇을 할 수
있는지, 어느 테넌트에 속하는지, 무엇이 바뀌었는지, 그리고 이를 관리할 관리자 표면.
`devslab-kit`은 그 계층을 한 번 제공하며, override 친화적입니다:

- **자동 구성.** 스타터를 추가하고 PostgreSQL을 가리킨 뒤 부팅.
- **Override 친화적.** 모든 기본 빈이 `@ConditionalOnMissingBean` — 직접 선언하면 어느
  조각이든 교체.
- **계약은 Java API.** 각 기능은 얇은 `-api` 계약과 `-core` 기본 구현으로 나뉩니다.
  `-api`에만 의존해 직접 구현을 제공할 수 있습니다.

## 다음 단계

- [설치](getting-started/installation.md) — 의존성 추가.
- [빠른 시작](getting-started/quick-start.md) — 동작하는 앱 부팅.
- [설정](reference/configuration.md) — 모든 `devslab.kit.*` 설정.
- [관리자 REST API](reference/admin-api.md) — `/admin/api/v1` 표면.

동반 [**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui)
(Vue 3 + PrimeVue)는 관리자 REST API 위에 바로 쓰는 콘솔입니다.
