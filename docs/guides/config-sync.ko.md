# 설정 동기화 (Config Sync)

**정의성 플랫폼 설정** — 권한, 역할(과 그 역할이 부여하는 권한 코드), 메뉴 — 을 대상 DB를
직접 손대지 않고, 이식 가능한 코드 기준 번들로 한 환경에서 다른 환경으로 승격합니다.

admin 콘솔은 RBAC 그래프 전체를 런타임에 관리할 수 있지만, 그 설정은 코드가 아니라
**데이터베이스**에 있습니다. 팀은 로컬에서 설계한 뒤 dev / staging / 운영 — 각자 자기 DB —
에 *같은* 구조가 필요해집니다. 설정 동기화가 이를 옮기는 일급 수단입니다. 배경과 설계는
[ADR 0003](../adr/0003-config-sync.md) 참고.

!!! warning "기본 off, 운영에선 금지"
    설정 동기화는 **dev/staging 편의 도구**로, 명시적으로 켜기 전까지 비활성이며 `prod` /
    `production` 프로파일에서는 **기동을 거부**합니다. 운영 설정은 배포 시 git 에 커밋된 번들을
    적용해 승격하지, 라이브 시스템에 즉석으로 push 하지 않습니다.

## 활성화

```yaml
devslab:
  kit:
    config-sync:
      enabled: true   # 기본 false — 끄면 엔드포인트·UI 전체가 비활성
```

`enabled=true` 인데 `prod`/`production` 프로파일이 활성이면, 기능을 조용히 끄는 대신 명확한
메시지와 함께 기동 단계에서 즉시 실패합니다.

## 번들에 담기는 것

| 포함(정의성) | 제외 |
| --- | --- |
| 권한 (`code`, 설명) | 감사 로그 (이력) |
| 역할 (`code`, 이름, **권한 코드**) | ABAC 정책 (이건 데이터가 아니라 *코드*) |
| 메뉴 (`code`, 부모 코드, 라벨, 경로, 아이콘, 필요 권한 코드, 순서) | 비밀번호 / 시크릿 |
| 사용자 — **옵트인 시에만**, login id 기준, 역할 코드 + **비밀번호 없음** | |

모든 것이 DB UUID 가 아니라 **자연 코드**로 키잉되므로, id 가 다른 다른 환경에도 그대로
적용됩니다.

## 엔드포인트

| | |
| --- | --- |
| `GET /admin/api/v1/config/export?tenantId={t}&includeUsers=false` | 번들을 JSON 으로 반환. |
| `POST /admin/api/v1/config/import?mode=merge&dryRun=true&includeUsers=false` | 번들 적용; 섹션별 diff 반환. |

## 모드

- **`merge`**(기본) — 추가형. 생성·수정만 하고 **삭제하지 않으며**, 역할의 기존 권한도 회수하지
  않음. 멱등: 같은 번들을 다시 적용해도 변화 없음.
- **`mirror`** — 대상을 *번들과 정확히 일치*시킴. merge 에 더해 역할 권한을 재조정(번들에 없는
  권한 회수)하고, 번들에 없는 정의성 엔터티를 **삭제**:
    - **메뉴**는 leaf-first(자식 먼저, 부모 나중)로 삭제;
    - **사용자에게 할당된 역할은 skip** — mirror 가 사용자 역할을 함부로 떼지 않음;
    - **권한**은 테넌트 역할에서 회수 후 삭제.

!!! danger "미러는 삭제합니다"
    `mirror` 는 항목을 제거합니다. 적용 전 항상 **dry-run** diff 를 확인하고, 단일 테넌트 배포
    환경에서만 권장합니다(권한은 전역).

## 먼저 dry-run

`dryRun=true` 가 **기본**입니다. import 는 전체 diff 를 계산하고 아무것도 기록하지 않습니다.
결과는 섹션별(`permissions` / `roles` / `menus` / `users`)로 다음을 보고합니다:

- **생성(created)**, **수정(updated)**, **삭제(deleted, 미러 전용)**, **건너뜀(skipped — 사용
  중인 역할, 또는 이미 존재하는 사용자)**.

미리보기가 의도와 맞으면 `dryRun=false` 로 실제 적용합니다.

## 사용자 동기화 (옵트인)

`includeUsers=true` 시:

- **export** 는 사용자를 login id 기준으로 — 이메일·상태·역할 코드 — 내보내되 **비밀번호는
  절대 포함하지 않음**.
- **import** 는 **생성 전용**: 없는 사용자만 사용 불가 비밀번호 + `mustChangePassword` 로 생성한
  뒤 코드로 역할을 할당. **기존 사용자는 절대 덮어쓰지 않음**(`skipped` 로 보고). 비밀번호는
  이후 admin 콘솔에서 설정.

사용자는 운영 데이터입니다. 새 환경에 계정을 시드할 의도가 아니면 `includeUsers` 는 꺼 두세요.

## 권장 워크플로

1. 로컬 DB 에서 설정 설계(admin 콘솔 또는 API).
2. 번들 **export**(정의성만이면 `includeUsers=false`).
3. 번들 JSON 을 git 에 커밋 — 이제 리뷰·버전 관리되는 설정.
4. 대상에서 import **dry-run** 후 diff 확인.
5. 적용(`dryRun=false`). 추가/수정은 `merge`, 대상을 번들과 정확히 일치시킬 때만 `mirror`.

## Admin 콘솔

[admin 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)에 전체 흐름을 다루는 **Config
Sync** 페이지가 있습니다: export(보기 / 다운로드 / 복사), import(붙여넣기·업로드 → dry-run
diff → 적용), `merge`/`mirror` 전환, 사용자 동기화 토글.
