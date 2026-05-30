# ADR 0001 — 환경별 최초 관리자 부트스트랩

- **상태:** 제안(Proposed)
- **날짜:** 2026-05-30
- **언어:** [English](0001-bootstrap-admin.md) · [한국어](0001-bootstrap-admin.ko.md)

## 배경

`devslab-kit` 는 인증, RBAC + 그룹 + ABAC, 멀티테넌시, 메뉴, 감사 로그, admin
REST API 를 제공하고, 이를 `devslab-kit-admin-ui` 대시보드가 소비한다.

여기엔 닭과 달걀 문제가 있다: 새 데이터베이스에는 **사용자 계정이 0개**라서
아무도 대시보드에 로그인할 수 없고, 그러면 메뉴를 만들거나 권한을 부여하거나
*진짜* 관리자를 추가할 수도 없다. 모든 시스템에는 **최초 관리자**를 만드는
경로가 반드시 있어야 한다.

어려운 점은 이걸 **영구 백도어를 남기지 않으면서** 하는 것이다. 게다가
*동일한 산출물(jar)* 이 요구사항이 다른 세 환경을 거쳐 이동한다:

| 환경 | 운영자가 원하는 동작 |
| --- | --- |
| **local-dev** | `admin / admin`, 즉시 로그인, 가능하면 비번 변경 단계도 건너뛰고 빠르게 |
| **staging** | 부트스트랩하되 비번은 주입, 강제 변경 ON (운영 리허설) |
| **production (운영)** | 고정 기본 비번 절대 금지. 비번 주입 + 강제 변경, 또는 자동 부트스트랩 자체를 끔 |

### "그냥 Spring 프로파일 쓰면 되지" 가 부족한 이유

`@Profile("dev")` 로 부트스트랩을 거는 건 직관적이지만 결함이 있다:

- **프로파일은 consumer 의 것이다.** 라이브러리가 프로파일 이름에 의존할 수 없다 —
  consumer 마다 `local` / `dev` / `development` 로 제각각이다.
- **프로파일은 on/off 만 표현한다.** "비번을 주입했는가?", "강제 변경이 켜졌는가?"
  같은 조건을 담을 수 없다.
- **프로파일 실수는 보안 사고다.** 운영에서 실수로 `dev` 프로파일을 켜면 조용히
  `admin/admin` 백도어가 생성된다.

업계 표준은 **명시적 property + 안전 가드의 조합**이며, 프로파일은(쓰더라도)
그 property 를 켜는 consumer 쪽 *수단*일 뿐 — kit 의 트리거가 되어선 안 된다.

## 결정

### 1. 부트스트랩은 property 로 제어, 기본값 OFF

새 `devslab.kit.bootstrap.*` 설정 블록을 두되, consumer 가 명시적으로 켜기
전까지는 비활성:

```yaml
devslab.kit.bootstrap:
  enabled: false                 # 기본값 — 설정 없는 운영 배포는 부트스트랩 안 함
  tenant-id: default             # 최초 관리자가 속할 테넌트
  admin-login-id: admin
  admin-password:                # 비우면 → 랜덤 생성 후 로그에 1회 출력
  admin-email: admin@example.com
  must-change-password: true     # 기본 ON — 첫 로그인 시 비번 변경 강제
  role-code: PLATFORM_ADMIN
```

기본이 `enabled: false` 이므로, 부트스트랩 설정 없이 산출물을 배포하면(=운영
기본) 아무것도 생성하지 않는다. 백도어가 실수로 생길 수 없다.

### 2. 고정 기본 비번 없음 — 비우면 "랜덤 생성 + 로그 1회"

`enabled: true` 인데 `admin-password` 가 비어 있으면, 러너가 강력한 랜덤 비번을
생성해 부팅 로그에 **정확히 한 번** 출력한다:

```
============================================================
 devslab-kit bootstrap: created first admin
   tenant : default
   login  : admin
   password (shown ONCE — copy it now): a8Kx29...
   This account must change its password on first login.
============================================================
```

GitLab / Jenkins / Sonatype 방식이다. 고정 `admin/admin` 은 운영자가 직접
`admin-password: admin` 이라고 적었을 때**만** 생긴다 — 그게 바로 local-dev
프로파일이 하는 일이고, 운영 설정이 절대 해선 안 되는 일이다.

### 3. 첫 로그인 시 비번 변경 강제

사용자 계정에 `must_change_password` 플래그를 추가한다(부트스트랩 관리자는 기본
`true`). 플래그가 켜져 있는 동안:

- 로그인 자체는 인증되고 토큰도 발급되지만, 토큰 / 로그인 응답에
  `mustChangePassword: true` 가 실린다.
- 대시보드는 그 플래그를 보고 사용자를 **비번 변경 화면**으로 보내고, 비번을
  바꾸기 전까지 다른 모든 라우트를 막는다.
- 새 self-service 엔드포인트 `POST /admin/api/v1/auth/change-password`
  (기존 비번 + 새 비번)가 플래그를 해제한다. 이는 기존 "관리자가 남의 비번을
  리셋하는" 엔드포인트와 구분된다.

이로써 운영자 시나리오 전체가 작동한다: 부트스트랩 비번으로 로그인 → 새 비번
강제 설정 → 이제 일반 관리자 → 메뉴 생성 / 권한 부여 / 진짜 관리자 추가 →
(원하면) 부트스트랩 관리자 비활성 또는 삭제.

### 4. 환경별 사용법 (consumer 쪽)

kit 은 환경 비의존으로 유지하고, consumer 가 프로파일별 설정 파일에 의도를 표현:

```yaml
# application.yml  (공통) — 여기에 아무것도 없음 = 운영 기본 안전

# application-local.yml
devslab.kit.bootstrap:
  enabled: true
  admin-password: admin
  must-change-password: false        # 로컬: 바로 로그인

# application-staging.yml
devslab.kit.bootstrap:
  enabled: true
  admin-password: ${BOOTSTRAP_ADMIN_PW}   # 주입된 시크릿
  must-change-password: true

# production (운영)
#   옵션 A: staging 과 동일하게 시크릿 주입 + 강제 변경.
#   옵션 B: bootstrap.enabled=false 로 두고, 최초 관리자를 별도 경로
#           (일회성 잡 / SQL / CLI)로 프로비저닝 → 실행 중인 앱에는
#           부트스트랩 경로 자체가 없음.
```

### 5. 멱등성 + 운영 안전핀

- **멱등(idempotent):** 러너는 `(tenant, loginId)` 로 기존 사용자를 확인하고
  있으면 생성을 건너뛴다. staging DB 를 운영으로 승급하거나 재시작해도 러너는
  no-op 으로 재실행된다.
- **안전핀(선택, 기본 ON):**
  `devslab.kit.bootstrap.fail-on-default-password-in-prod: true` — 활성
  프로파일에 `prod`/`production` 이 있고 **그리고** 결정된 비번이 잘 알려진 약한
  값(`admin`, `password`, `changeme` …)과 같으면, 앱이 명확한 메시지와 함께
  부팅에 실패한다. 이건 주 통제가 아니라 백스톱이다(주 통제는 "고정 기본값이
  애초에 없다").

### 6. 프론트 대시보드 영향 (향후 고려)

- **현재:** 대시보드가 `mustChangePassword` 를 처리해야 한다 — 비번 변경
  화면으로 보내는 가드 + 새 엔드포인트를 호출하는 작은 화면. (후속 UI PR 로 추적.)
- **추후 — 가이드형 first-run / 설치 마법사:** `admin/admin` 대신, 프로비저닝되지
  않은 인스턴스의 첫 대시보드 방문에 일회성 "최초 관리자 생성" 폼을 띄울 수 있다
  (Jenkins 설치 화면, GitLab root 비번 화면 같은 방식). 대화형 설치에서는
  config-부트스트랩을 이 마법사가 대체하고, headless / 자동 배포에는 property
  경로가 그대로 남는다. 본 ADR 범위 밖이지만, 부트스트랩 계약이 이를 수용할
  여지를 남기도록 기록한다(예: 마법사가 분기할 수 있는
  `GET /admin/api/v1/bootstrap/status` → `{ initialized: boolean }`).

## 결과(Consequences)

**긍정**
- 산출물 하나로 세 환경, 재빌드 없음 — 동작이 코드가 아니라 설정이다.
- 기본 안전(OFF)이고, 켜더라도 고정 비번이 새지 않는다.
- "로그인 → 비번 교체 → 인계 → 부트스트랩 관리자 제거" 전체 라이프사이클을
  표현할 수 있다.
- `sample-app` 의 현재 `SampleSeedRunner` 가 이 한 메커니즘으로 흡수된다
  (dev 설정에서 `bootstrap.enabled=true, admin-password=admin,
  must-change-password=false`), 중복 로직 제거.

**부정 / 비용**
- 움직이는 부품이 여럿: 스키마 마이그레이션(`must_change_password`), identity
  필드, self-service change-password 엔드포인트 + 로그인 응답 필드, autoconfig
  러너, UI 가드 + 화면. 여러 PR 로 나누는 게 낫다(구현 계획 참조).
- 강제 변경은 명시적으로 끄지 않는 한 dev 에서 로그인 왕복을 한 번 추가한다.
- 랜덤-비번-로그 방식은 운영자가 첫 부팅 로그를 읽을 수 있다고 전제한다 —
  문서에 눈에 띄게 명시.

## 구현 계획 (PR 분할)

1. **identity: `must_change_password`** — 마이그레이션 `V11`, 엔티티 필드,
   `CurrentUser` 필드, 로그인 응답 + JWT 클레임에 노출.
2. **identity: self-service change-password** — 기존 비번 검증 후 새 비번 설정
   + 플래그 해제하는 `LocalLoginService` / 계정 서비스 메서드;
   `POST /admin/api/v1/auth/change-password`.
3. **autoconfigure: `BootstrapAutoConfiguration` + `DevslabKitBootstrapRunner`**
   — property 블록, 랜덤 비번 생성 + 1회 로그, 멱등 프로비저닝, 운영 안전핀.
   `sample-app` 이 이걸 쓰도록 전환하고 `SampleSeedRunner` 삭제.
4. **admin-ui: 강제 변경 가드 + 화면** — `mustChangePassword` 라우터 가드,
   비번 변경 뷰, 새 엔드포인트 연동.
5. **docs:** 양쪽 README 의 "first run" 섹션이 이 ADR 을 참조하도록 갱신;
   환경별 설정 스니펫 추가.

## 검토한 대안

- **프로파일 게이트 부트스트랩(`@Profile("dev")`):** 기각 — *배경* 참조.
- **고정 기본값으로 `first-run` 자동 생성, opt-in 없음:** 기각 — 라이브러리의
  고정 기본값은 잠재적 운영 백도어다.
- **비번 미주입 시 `fail-fast`(dev 에서도):** 가능하고 더 엄격하지만 local-dev
  편의가 나쁘다; 랜덤-비번-로그 경로가 같은 운영 안전을 더 친절한 기본값으로
  제공한다. consumer 의 선택지로 남긴다(항상 비번을 주입하면 됨).
- **부트스트랩 아예 없음(consumer 책임):** 가장 안전하지만, 이 질문을 촉발한
  "스타터 넣고 바로 시작" 목표를 달성하지 못한다.
