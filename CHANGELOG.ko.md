# 변경 이력 (Changelog)

이 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르며,
[유의적 버전(SemVer)](https://semver.org/lang/ko/)을 준수합니다.

라이브러리 메이저는 Spring Boot 메이저와 정렬됩니다: `4.x.y`는 Spring Boot 4.x를 대상으로 합니다.

English: [CHANGELOG.md](CHANGELOG.md)

## [Unreleased]

## [0.5.0] — 2026-06-03

### Added
- **설정 기반 RBAC 시드.** `devslab.kit.bootstrap.seed`가 스타터 권한·역할(부여 포함)을 부팅 시
  멱등하게 프로비저닝해, consumer가 콘솔에서 일일이 만들 필요가 없습니다. 도메인 권한 코드와
  그것을 묶는 역할을 선언하세요:
  ```yaml
  devslab:
    kit:
      bootstrap:
        enabled: true
        seed:
          permissions: [tasks.read, tasks.write, tasks.update, tasks.delete]
          roles:
            viewer: [tasks.read]
            editor: [tasks.read, tasks.write, tasks.update]
            owner:  [tasks.read, tasks.write, tasks.update, tasks.delete]
  ```
  추가형(additive)입니다 — 매 부팅 시 없는 것을 만들고 나열된 grant를 추가하되, 회수·삭제는
  하지 않습니다. 역할이 참조하는 권한은 자동 생성됩니다. 권한은 전역, 역할은
  `bootstrap.tenant-id`에 생성됩니다. [부트스트랩 가이드](guides/bootstrap.md#seed) 참고.
- **JWT 테넌트 리졸버.** `devslab.kit.tenant.resolver: jwt`가 이제 부팅 시 실패하는 대신, kit이
  발급한 bearer 토큰의 `tenant` 클레임에서 활성 테넌트를 resolve합니다(토큰이 없으면
  `default-tenant-id`로 폴백). kit 자체 HS256 토큰을 읽으며, 외부 OAuth2 / OIDC 토큰 검증은 별도
  과제입니다. [멀티테넌시 가이드](guides/tenancy.md) 참고.

## [0.4.2] — 2026-06-03

### Added
- **사용자의 역할·그룹 조회.** `GET /admin/api/v1/users/{id}/roles`,
  `GET /admin/api/v1/users/{id}/groups` 가 사용자에게 부여된 역할/그룹 id 를 반환합니다.
  이로써 admin 콘솔이 **사용자 화면에서** 그 사용자의 접근권한을 관리할 수 있습니다(부여/회수는
  기존대로 역할·그룹 리소스에: `POST/DELETE /roles/{roleId}/users/{userId}`,
  `/groups/{groupId}/members/{userId}`). 안정적인 `[{ "value": "<uuid>" }]` 배열로 반환.

## [0.4.1] — 2026-06-03

### Fixed
- **감사 로그 검색이 안정적인 페이지 구조를 반환.** `GET /admin/api/v1/audit-logs` 가 raw
  `PageImpl` 대신 Spring Data `PagedModel`(`{ content, page: { size, number, totalElements,
  totalPages } }`)로 직렬화됩니다. "Serializing PageImpl instances as-is is not supported" 경고가
  사라지고 JSON 계약이 안정화됩니다. kit 자체 엔드포인트에만 적용(전역 `@EnableSpringDataWebSupport`
  없음)이라 소비자 앱의 자체 페이징엔 영향이 없습니다.

## [0.4.0] — 2026-06-03

### Added
- **환경 간 설정 동기화 (ADR 0003).** 정의성 플랫폼 설정 — 권한, 역할(+ 권한 코드), 메뉴 —
  을 대상 DB를 직접 손대지 않고, 이식 가능한 코드 기준 번들로 한 환경에서 다른 환경으로 승격.
  - `GET /admin/api/v1/config/export`, `POST /admin/api/v1/config/import`.
  - **merge**(기본, 추가형 — 생성·수정만, 삭제 없음) 와 **mirror**(대상을 번들과 일치: 역할
    권한 재조정 + 번들에 없는 정의성 엔터티 삭제 — 메뉴 leaf-first; 사용자에게 할당된 역할은
    skip; 권한은 revoke 후 삭제).
  - **기본 dry-run** — import 는 섹션별 diff(생성 / 수정 / 삭제 / 건너뜀)를 반환하고
    `dryRun=false` 가 아니면 아무것도 기록하지 않음.
  - **옵트인 사용자 동기화**(`includeUsers`, 기본 off): export 는 사용자를 login id 기준으로
    **비밀번호 없이** 내보내고, import 는 생성 전용이라 기존 사용자를 절대 덮어쓰지 않음.
  - **기본 off**(`devslab.kit.config-sync.enabled`) + **운영 프로파일**(`prod`/`production`)
    에서 **기동 거부**(ADR 0003 §5) — 설정은 배포 시 커밋된 번들로 승격하지, 운영에 즉석
    push 하지 않음.
  - [admin 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)의 **Config Sync** 페이지가
    export / import / dry-run diff / 적용을 담당.

### Notes
- 마이그레이션 불필요: 설정 동기화는 테이블을 추가하지 않으며, 명시적으로 켜기 전까지 비활성.

## [0.3.0] — 2026-06-02

### 변경됨 (Changed)
- **킷의 Flyway 마이그레이션을 이제 전용 스키마 히스토리 테이블에서 실행합니다.** 기본
  `classpath:db/migration`(`flyway_schema_history`)에서 `classpath:db/devslab-kit`
  (`devslab_kit_schema_history`)로 옮겨, `KitFlywayAutoConfiguration`이 전용 Flyway로
  프로그램적으로 적용합니다(`Flyway` 빈이 아니라서 소비자의 자동 구성 Flyway를 죽이지 않음).
  소비자의 기본 `classpath:db/migration` + `flyway_schema_history`는 온전히 앱 몫으로 남아,
  소비자는 자기 `V1__*.sql`을 **버전 충돌 없이** 실을 수 있고, 킷은 릴리스마다 마이그레이션을
  추가해도 소비자의 순서를 건드리지 않습니다. 소비자 Flyway가 먼저(빈 스키마에서) 돌고 킷이
  두 번째로 `baselineOnMigrate`로 돕니다. **소비자 쪽 설정은 전혀 필요 없습니다.**
  - **업그레이드:** 기본 `flyway_schema_history`에 이미 킷 마이그레이션을 적용한 `0.2.x`
    데이터베이스는 재생성해야 합니다(pre-1.0, 실제로는 데모/로컬 DB뿐). 새 DB는 조치 불필요.

### 변경됨 — 호환성 깨짐 (Changed, breaking)
- **admin API가 이제 모든 오류를 RFC 7807 `ProblemDetail`**(`application/problem+json`)로
  반환합니다 — 기존 `ApiError` 본문을 대체. 필드는 `type` / `title` / `status` / `detail`이며,
  필드별 검증 메시지는 `errors` 확장에 담깁니다. 사람이 읽는 메시지는 `detail`(없으면 `title`)에서
  읽으세요 — 기존 최상위 `message` 필드는 사라졌고 `kr.devslab.kit.admin.ApiError` 레코드도
  제거됐습니다. `devslab-kit-admin-ui`는 `detail`을 읽도록 함께 수정됐습니다.

## [0.2.1] — 2026-06-02

### 변경됨 (Changed)
- **OpenAPI / Swagger UI를 이제 스타터에 포함** — `0.2.0`은 springdoc을 `compileOnly`로
  배포해, 소비자가 직접 springdoc을 추가해야만 Swagger UI가 떴습니다. 이제 스타터가
  `springdoc-openapi-starter-webmvc-ui`를 `api`로 의존하므로 스타터만으로
  `/swagger-ui`와 `/v3/api-docs`가 올라옵니다 — "스타터만 추가하면 다 됨" 약속에 부합.
  `devslab.kit.openapi.enabled=false`로 끄거나 `org.springdoc` 의존성을 `exclude`해
  jar를 제거할 수 있습니다(설정 레퍼런스에 문서화).

## [0.2.0] — 2026-06-02

### 추가됨 (Added)
- **OpenAPI / Swagger UI 자동 구성** — springdoc-openapi가 classpath에 있으면(킷은
  `compileOnly`로 선언, 소비자가 `springdoc-openapi-starter-webmvc-ui`를 추가해 opt-in)
  설정 없이 `/swagger-ui.html`과 `/v3/api-docs`가 올라오고 `/admin/api/v1/**`
  엔드포인트가 하나의 그룹으로 묶입니다. 의존성을 제거하지 않고
  `devslab.kit.openapi.enabled=false`로 끌 수 있습니다(프로덕션에서 일반적). `OpenAPI`
  문서 빈과 관리자 `GroupedOpenApi` 빈은 `@ConditionalOnMissingBean`이라 소비자가
  재정의 가능. springdoc `3.0.x`(Spring Boot 4 라인) 대상.

## [0.1.0] — 2026-06-01

첫 공개 릴리스.

### Added
- **Maven Central 배포** — 모든 라이브러리 모듈을 vanniktech maven-publish 플러그인으로
  Maven Central에 배포합니다 (Central Portal, 서명, `v*` 태그 시 자동 릴리스).
  `release.yml`이 아티팩트를 배포하고 GitHub Release를 생성합니다.
- **설정 없는 JPA 자동 등록** — 스타터가 자신의 `@Entity` 타입과 Spring Data
  리포지토리를, 임의의 패키지에서 평범한 `@SpringBootApplication`으로 실행하는
  소비자에게 자동 등록합니다. `@EntityScan`·`@EnableJpaRepositories`·`scanBasePackages`
  불필요. 스캐닝을 대체하지 않고 넓히므로 소비자 자신의 엔티티·리포지토리도 그대로
  동작합니다(`com.example.consumer`의 외부 소비자 통합 테스트로 검증).
- **설정 없는 관리자 API 웹 레이어** — 관리자 컨트롤러·예외 핸들러·보안 체인도 자동
  등록되어, 스타터만으로 `/admin/api/v1/**`가 올라옵니다(서블릿 웹 앱). 마찬가지로
  컴포넌트 스캔 설정이 필요 없습니다.
- **관리자 API 인가(authorization) 강제** — 모든 `/admin/api/v1/**` 엔드포인트가
  매핑된 `admin.*` 권한을 요구합니다(읽기 → `*.read`, 변경 → `*.write`). kit의 보안
  체인이 호출자의 실효 권한(매 요청마다 역할·그룹에서 해석)으로 강제하므로, 권한
  부여/회수가 다음 호출에 즉시 반영됩니다. 최초 관리자 부트스트랩이 모든 `admin.*`
  권한을 `PLATFORM_ADMIN`에 시드하므로 시드된 관리자는 즉시 전체 API를 사용할 수
  있습니다. `login`과 `bootstrap/status`는 공개로 유지됩니다.
- **플러그형 캐시** (ADR 0002) — `devslab.kit.cache.type` = `in-memory` / `redis` /
  `none`. Redis 백엔드가 JSON 직렬화를 직접 책임지므로 (`Serializable` 불필요, 직렬화기
  설정 불필요) 사용자별 메뉴 캐시도 자체 맵 대신 이 공유 캐시 매니저를 사용합니다.
- **최초 관리자 부트스트랩** (ADR 0001) — opt-in, 프로퍼티 기반 프로비저닝으로 빈 데이터베이스가
  영구적인 백도어 없이 사용 가능한 대시보드에 도달합니다. `devslab.kit.bootstrap.*`(기본 OFF)가
  첫 부팅 시 테넌트, 전체 `admin.*` 권한 세트를 가진 `PLATFORM_ADMIN` 역할, 관리자 사용자 1명을
  멱등하게 생성합니다. 빈 비밀번호는 강력한 랜덤 비밀번호를 생성해 한 번 로깅하며, prod 안전장치는
  `prod`/`production` 프로파일에서 약한 비밀번호를 거부합니다.
- **비밀번호 강제 변경** — 사용자 계정의 `must_change_password` 플래그(`V11`)를 `CurrentUser`,
  JWT 클레임, 로그인 응답으로 노출합니다. 셀프 서비스 `POST /admin/api/v1/auth/change-password`가
  이전 비밀번호를 검증하고 새 비밀번호를 설정한 뒤 플래그를 지우고 토큰을 재발급합니다.
- **부트스트랩 상태 프로브** — 인증 없는 `GET /admin/api/v1/bootstrap/status`가
  `{ initialized: boolean }`을 반환합니다. 향후 가이드형 최초 실행 / 설정 마법사의 분기점입니다
  (ADR 0001 §6).

### Fixed
- **JWT 검증이 주입된 `Clock`을 사용하도록 수정** — `JjwtAuthTokenService.parse()`가 토큰 만료를
  주입된 시계가 아니라 실제 시스템 시계로 검증하고 있었습니다. 이로 인해 고정 시계로는 검증을
  테스트할 수 없었고 `issue()`와 비대칭이었습니다. 운영 동작은 그대로입니다 (런타임은 양쪽 모두
  `Clock.systemUTC()` 사용).

### Changed
- `sample-app`이 `SampleSeedRunner`를 끄고 스타터의 `devslab.kit.bootstrap.*` 러너로
  전환했습니다 (로컬 개발 형태: `admin/admin`, `must-change-password=false`).

### Added (초기 스캐폴드)
- 초기 프로젝트 스캐폴드 (Spring Boot 4 + Java 21 + Gradle).
- 기본 의존성: Spring Web MVC, Spring Security, Spring Data JPA, Spring Data Redis,
  Flyway (PostgreSQL), Spring Boot Actuator, GraalVM Native, Testcontainers (PostgreSQL + Redis),
  Docker Compose 지원.
- 기본 패키지 `kr.devslab.kit`.
- Gradle 멀티모듈 분리 (계획 문서 §5):
  `devslab-kit-core`, `-{identity,access,tenant,menu,audit}-{api,core}`,
  `-autoconfigure`, `-spring-boot-starter`, `-sample-app`.
- 코어 값 객체: `UserId`, `TenantId`, `RoleId`, `PermissionId`, `MenuId`, `PublicId`,
  `DevslabKitException`.
- Tenant: `TenantContext`, `TenantContextHolder`, `TenantResolver`, `TenantMode` (api) +
  `DefaultTenantContextHolder`, `FixedTenantResolver` (core). AutoConfig override 패턴을
  증명하는 데 사용한, 완전히 배선된 첫 수직 슬라이스.
- Identity (api): `CurrentUser`, `CurrentUserProvider`, `UserStatus`, `LoginCommand`,
  `LoginResult`, `UserAccountView`, `PasswordHasher`, `LoginFailureReason`,
  `AccountLoginException`, `LoginSucceededEvent`, `LoginFailedEvent`,
  `UserAccountCreatedEvent`.
  Identity (core): `PlatformUserAccountEntity` + `JpaPlatformUserAccountRepository`,
  `BCryptPasswordHasher`, `LocalLoginService`, `PlatformUserAccountService`,
  `DefaultCurrentUserProvider`, `V1__platform_user_account.sql`.
- Access (api): `Permission`, `Role`, `PermissionChecker`, `PermissionDeniedException`.
  Access (core): `Platform{Role,Permission,UserRole,RolePermission}Entity` + Jpa 리포지토리,
  `UserRoleService`, `RolePermissionService`, `DefaultPermissionChecker`, `V2__platform_access.sql`.
- Menu (api): `MenuItem`, `MenuTree`, `MenuProvider`.
  Menu (core): `PlatformMenuEntity` + `JpaPlatformMenuRepository`, `MenuTreeBuilder`,
  `PermissionBasedMenuFilter`, `DefaultMenuProvider`, `V3__platform_menu.sql`.
- Audit (api): `AuditEvent`, `AuditActor`, `AuditAction`, `AuditTarget`, `AuditEventPublisher`.
  Audit (core): `PlatformAuditLogEntity` + `JpaPlatformAuditLogRepository`, `AuditLogService`
  (Jackson 직렬화 메타데이터), `DefaultAuditEventPublisher`, `V4__platform_audit_log.sql`.
- `DevslabKitProperties` (`devslab.kit.*` 접두사) + `@ConditionalOnMissingBean` override를 갖춘
  5개 `AutoConfiguration`: `Tenant`, `Identity`, `Access`, `Menu`, `Audit`.
- `devslab-kit-sample-app`이 8개 스타터 빈(`TenantResolver`, `TenantContextHolder`,
  `CurrentUserProvider`, `PasswordHasher`, `LocalLoginService`, `PermissionChecker`,
  `MenuProvider`, `AuditEventPublisher`)과 BCrypt 왕복을 스모크 테스트합니다.

### Notes
- SB 4.1-SNAPSHOT / Java 25 대신 Spring Boot 4.0.6(릴리스)와 Java 21을 대상으로 합니다.
  SB SNAPSHOT + Java 25 조합이 일부 환경에서 IntelliJ Gradle 통합을 깨뜨려, 릴리스 버전으로
  고정해 import 경로를 예측 가능하게 유지합니다. SB 4.1.x 릴리스가 나오면 Java 25를 재검토합니다.
