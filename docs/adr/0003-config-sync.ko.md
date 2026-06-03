# ADR 0003 — 환경 간 플랫폼 설정 동기화 (라이브 push가 아니라 export/import)

- **상태:** 제안됨(Proposed)
- **날짜:** 2026-06-03
- **구현(제안):** `devslab-kit-admin-api`의 설정 엔드포인트(`config/export`,
  `config/import`), `-core`의 export/import 서비스, `devslab-kit-autoconfigure`의 게이팅
  (`devslab.kit.config-sync.*`), 그리고 `devslab-kit-admin-ui`의 "Config Sync" 페이지.
  `0.4.0` 목표. 아래 PR 분해 참고.

## 배경 (Context)

ADR 0001/0002와 0.3.x 라인을 거치며, admin 콘솔은 이제 RBAC 그래프 전체 — 권한, 역할,
역할→권한 매핑, 그룹, 메뉴 — 를 런타임에 관리할 수 있다. 그 모든 것은 admin API를 통해
**데이터베이스**에 저장된다.

여기서 생기는 문제: 그 설정은 **코드가 아니라 DB 상태**다. 팀은 로컬(로컬 DB)에서 설정을
설계한 뒤, dev / staging / 운영 — 각자 자기 DB를 가진 — 에 *같은* 구조 설정이 필요해진다.
이를 승격(promote)할 일급 수단이 없다. 현재 재현 가능한 유일한 경로는 손으로 짠 seeder
(소비자 코드의 `ApplicationRunner`가 멱등하게 upsert)뿐인데, 동작은 하지만 소비자마다 매번
새로 짜야 하고 틀리기 쉽다.

플랫폼 데이터는 두 종류로 나눠 다뤄야 한다:

- **정의/구조(Definitional)** — 권한, 역할, 역할→권한 매핑, 메뉴. *모든 환경에서 동일해야*
  한다. 애플리케이션의 **권한 모델(capability model)** 이다.
- **운영/인스턴스(Operational)** — 실제 사용자 계정, 누가 무엇을 할당받았는지, 감사 이력.
  *환경마다 다르다*(로컬엔 테스트 유저, 운영엔 실제 유저).

필요한 것: **정의 설정**을 환경 간에 안전하게 옮기는(그리고 배포 시 적용되는 코드로 표현하는)
일급 수단 — 운영 데이터나 자격증명을 끌고 가지 않고, 소비자마다 seeder를 다시 만들지 않으면서.

## 결정 (Decision)

### 1. 스코프 — 번들이 담는 것

- **항상(정의):** 권한(`code`), 역할(`tenant + code`), 역할→권한 매핑(코드 기준), 메뉴
  (`tenant + code`, 부모와 필요권한을 **코드로** 참조).
- **선택, 가드(운영):** 사용자 + 그 역할/그룹 할당. **기본 off.** 켜도 비밀번호 해시는 **절대**
  포함하지 않고, target에 이미 있는 사용자는 **절대** 덮어쓰지 않는다(신규만 삽입). 이 부분이
  유일한 위험 지점 — §5 참고.
- **절대 안 함:** 감사 로그(환경별 이력).
- **ABAC 정책은 스코프 밖** — 정책은 *코드*(`PolicyEvaluator` 빈)지 데이터가 아니다. JSON
  번들은 동작을 담을 수 없다. Policies 화면은 정책을 나열하고 dry-run 테스트할 뿐, 정책을
  "정의"하는 건 앱의 배포다.
- **테넌트:** 단일 테넌트에서는 번들이 그 한 테넌트(`default`)로 스코프된다. 테넌트 행 자체는
  환경 설정이라 기본적으로 동기화하지 않는다. 멀티 테넌트에서는 선택한 테넌트 단위로 실행.

### 2. 메커니즘 — 라이브 API push가 아니라 export/import 번들

- **`GET /admin/api/v1/config/export`** → 스코프 내 설정을 **자연키(코드)** 기준으로 담은 JSON
  문서 하나(버전 있는 스키마). DB UUID는 쓰지 않는다.
- **`POST /admin/api/v1/config/import?mode=merge&dryRun=true`** → 자연키로 upsert, target에서
  트랜잭션으로, **diff**(무엇이 생성/수정/삭제될지)를 반환. `dryRun=true`면 적용하지 않고 diff만.
- **편의 "target으로 push":** *source 백엔드*(브라우저가 아니라)가 `{ targetBaseUrl, targetToken }`
  으로 자기 export를 target의 import 엔드포인트에 POST — server-to-server라 CORS가 없고 target
  자격증명이 브라우저에 남지 않는다. export + import 위의 설탕일 뿐.

**왜 export/import가 기본 모델인가(라이브 push가 아니라):** 번들은 git에 커밋 가능한 산출물이다.
이 한 설계가 *배포/config-as-code* 문제까지 해결한다 — export한 JSON을 커밋해 PR로 리뷰하고,
배포 때 같은 import를 seeder로 돌린다. 한 기능, 세 용도: **환경 간 동기화, 버전 관리되는 설정,
배포 시드.** target에서 원자적이고, dry-run이 내장이며, 라이브 push의 다중 라운드트립 부분 실패를
피한다.

### 3. 식별 매칭 — UUID가 아니라 자연키

kit의 설정 엔티티는 이미 안정적인 자연키를 갖고 있어 환경 간 upsert가 잘 정의된다:

- 권한 `code`(전역 유일), 역할 `(tenant_id, code)`, 메뉴 `(tenant_id, code)`;
- 메뉴→권한은 이미 `required_permission_code`(id가 아니라 코드)로 저장됨.

번들은 모든 것을 코드로 참조한다(역할의 권한은 권한 코드로, 메뉴의 부모는 부모 코드로 …).
UUID는 환경마다 독립적으로 부여되며 경계를 넘지 않는다.

### 4. merge 의미 + 안전장치

- **`mode=merge`(기본):** 추가형, 멱등 upsert — 없으면 생성, 바뀌었으면 수정, **삭제는 안 함.**
  자기 추가 설정이 있을 수 있는 환경에 승격할 때 안전.
- **`mode=mirror`(opt-in):** target을 번들과 정확히 일치시킴, **삭제 포함.** 위험(target 전용
  설정을 지움). 명시적 opt-in + 항상 dry-run diff를 먼저.
- **UI에선 dry-run이 기본:** "역할 X 생성, 메뉴 Y 수정, 삭제 없음"을 보여주고 → 확인 → 적용.
- import는 `admin.config.sync` 권한(`admin.*`에 포함) 필요, **모든 import는 감사 로그에 기록.**

### 5. 게이팅 — 기본 off, dev 전용, 운영에선 fail-fast

- 프로퍼티 **`devslab.kit.config-sync.enabled`**(boolean, **기본 `false`**).
- 기능의 빈/엔드포인트는 `enabled=true` **그리고** 비-운영 프로필(예: `local`/`dev`)이 활성일
  때만 켜진다. 그 프로필을 안 쓰는 운영은 **구조적으로 켤 수 없다.** "운영인지 감지"하는 휴리스틱은
  **일부러 두지 않는다** — 질문을 뒤집어 켜려면 명시적 *dev* 신호를 요구한다.
- **운영** 프로필인데 `enabled=true`면 → **기동 시 fail-fast**(명확한 메시지). 조용히 끄지
  않는다: 조용한 전환은 미스config를 숨기고 시간을 낭비시킨다("왜 sync가 안 되지?").
- `enabled=true`는 환경 한정 설정(`application-local.yaml` / 개발자 환경변수)에만 둔다. 공용
  `application.yaml`엔 절대.
- **근거:** 라이브 sync/push 표면은 **dev/staging 편의**다. 운영 설정은 git에 커밋된 번들을
  배포 시 적용(시드 경로)하며, 이는 리뷰를 거친다 — 개발자 노트북에서의 즉석 push가 아니라.

### 6. 모듈 배치 / wire

- 엔드포인트는 `devslab-kit-admin-api`(`config/ConfigSyncController`); export/import/diff
  서비스는 `-core` 모듈(신규 `devslab-kit-config-sync-core` 또는 기존에 통합); 게이팅은
  `devslab-kit-autoconfigure`. admin 콘솔엔 **Config Sync** 페이지: export(다운로드),
  import(업로드 → dry-run diff → 적용), 선택적 push-to-URL.

## 결과 (Consequences)

**긍정**
- 권한 모델의 환경→환경 승격이 재현 가능.
- *같은* 번들이 버전 관리 설정 **이자** 배포 시드 — 소비자별 seeder 불필요.
- 자연키 설계가 이미 있어 환경 간 upsert가 잘 정의됨.
- 기본이 안전: off, dev 전용, `merge`(삭제 없음), dry-run 우선, 감사 기록.

**비용/부정**
- 진짜 기능: 엔드포인트 + 번들 직렬화 + upsert + dry-run diff + admin-ui 페이지 + 게이팅.
  번들 스키마는 호환성을 위해 **버전 관리** 필요.
- 손으로 설정을 고친 환경에 import할 때는 merge/dry-run 규율이 필요.
- 선택적 **사용자 동기화**가 위험 지점 — 비번 해시를 절대 안 싣고, 기존 target 사용자를 절대
  안 덮어써야 한다. 그래서 기본 off.

## 구현 계획 (PR 분해)

1. **번들 스키마 + `GET /config/export`**(정의 스코프) + `ConfigBundle` 레코드. 형태/코드 키 단위 테스트.
2. **`POST /config/import`** + `merge` + `dryRun` diff(코드 upsert, 트랜잭션). 실제 Postgres
   Testcontainers 테스트: 한 스키마에서 export → 새 스키마로 import → 동일성 검증.
3. **게이팅**: `config-sync.enabled` + dev 프로필 조건 + 운영 fail-fast(`FailureAnalyzer`로 친절한 메시지).
4. **admin-ui "Config Sync" 페이지**(export / import / dry-run diff / 적용) + 선택적 server-to-server push.
5. **`mirror` 모드 + 선택적 사용자 동기화**(가드) — `merge` 검증 후 별도 PR.
6. **문서**: 가이드("환경 간 플랫폼 설정 승격"), 배포 시드 사용법 문서화, 이 ADR을 **Accepted**로 전환.

## 검토한 대안 (Alternatives considered)

- **라이브 API-to-API push를 기본 모델로**(첫 직감): 기본으로는 기각 — 다중 라운드트립, 약한
  원자성, UI 주도 시 target 자격증명이 브라우저에, 그리고 git 산출물이 없음. export/import 위의
  선택적 server-to-server 설탕으로만 유지.
- **`platform_*` 테이블 `pg_dump`/복원**: 파이프라인으로는 기각 — UUID/FK를 끌고, merge·dry-run이
  없고, 전부-아니면-전무, 리뷰 불가. 일회성 수동 탈출구로는 OK.
- **소비자별 손수 seeder**(현 상태): 동작하나 매번 새로 짜야 하고 공유 불가. export 번들이 이를
  일반화하고, import 엔드포인트가 곧 재사용 가능한 seeder.
- **사용자·감사 로그까지 전부 동기화**: 기각 — 자격증명/PII를 운영으로 미는 건 보안 위험,
  감사는 환경별 이력. 기본은 정의만; 사용자는 opt-in + 가드.
