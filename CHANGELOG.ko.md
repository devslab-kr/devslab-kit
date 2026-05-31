# 변경 이력 (Changelog)

이 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르며,
[유의적 버전(SemVer)](https://semver.org/lang/ko/)을 준수합니다.

라이브러리 메이저는 Spring Boot 메이저와 정렬됩니다: `4.x.y`는 Spring Boot 4.x를 대상으로 합니다.

English: [CHANGELOG.md](CHANGELOG.md)

## [Unreleased]

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
