# 설정 동기화 (Config Sync)

**정의성 플랫폼 설정** — 권한, 역할(과 그 역할이 부여하는 권한 코드), 메뉴 — 을 대상 DB를
일일이 손으로 고치는 대신, 휴대 가능한 **코드 기반 번들**로 한 환경에서 다른 환경으로
승격합니다.

관리자 콘솔은 RBAC 그래프 전체를 런타임에 관리할 수 있지만, 그 설정은 코드가 아니라
**데이터베이스**에 있습니다. 팀이 로컬에서 설계한 뒤, 각자 DB를 가진 dev / staging /
production에 *같은* 구조가 필요해집니다. 설정 동기화가 이를 옮기는 일급 수단입니다. 근거와
설계는 [ADR 0003](../adr/0003-config-sync.md) 참고.

!!! warning "기본 off, 운영에서는 절대 안 됨"
    설정 동기화는 **dev/staging 편의 기능**으로, opt-in 하지 않으면 비활성이고, `prod` /
    `production` 프로파일에서는 **시작을 거부**합니다. 운영 설정은 라이브 시스템에 즉석으로
    푸시하는 게 아니라, git에 커밋된 번들을 배포 시 적용해서 승격합니다.

## 활성화

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    config-sync:
      enabled: true   # 기본 false — 아니면 엔드포인트+UI 전체가 비활성
```

`prod`/`production` 프로파일이 활성인데 `enabled=true`면, 조용히 비활성화하지 않고 시작 시
명확한 메시지와 함께 fail-fast 합니다.

## 번들에 담기는 것

| 포함(정의성) | 제외 |
| --- | --- |
| 권한(`code`, 설명) | 감사 로그(이력) |
| 역할(`code`, 이름, **권한 코드들**) | ABAC 정책(이건 *코드*지 데이터가 아님) |
| 메뉴(`code`, 부모 코드, 라벨, 경로, 아이콘, 필요 권한 코드, 순서) | 비밀번호 / 시크릿 |
| 사용자 — **opt-in 시에만**, login id 기준, 역할 코드 포함, **비밀번호 없음** | |

모든 것은 DB UUID가 아니라 **자연 코드**로 키잉되므로, 한 환경에서 export한 번들이 id가 다른
다른 환경에도 깔끔하게 적용됩니다.

## 엔드포인트

| | |
| --- | --- |
| `GET /admin/api/v1/config/export?tenantId={t}&includeUsers=false` | 번들을 JSON으로 반환. |
| `POST /admin/api/v1/config/import?mode=merge&dryRun=true&includeUsers=false` | 번들 적용; 섹션별 diff 반환. |

## 전체 왕복(round trip)

로컬에서 staging으로 설정 옮기기 — 복붙:

```bash
# 1. 로컬 — 정의성 번들을 파일로 export
curl -G localhost:8080/admin/api/v1/config/export \
  -H 'Authorization: Bearer <local-token>' \
  --data-urlencode 'tenantId=default' \
  --data-urlencode 'includeUsers=false' \
  -o config-bundle.json

# 2. 커밋해서 승격을 리뷰 가능·버전 관리되게
git add config-bundle.json && git commit -m "chore: rbac bundle"

# 3. staging — 먼저 DRY-RUN(아무것도 안 쓰고 diff 반환)
curl -X POST 'https://staging.example.com/admin/api/v1/config/import?mode=merge&dryRun=true' \
  -H 'Authorization: Bearer <staging-token>' \
  -H 'Content-Type: application/json' \
  --data-binary @config-bundle.json

# 4. diff 검토 후 실제 적용
curl -X POST 'https://staging.example.com/admin/api/v1/config/import?mode=merge&dryRun=false' \
  -H 'Authorization: Bearer <staging-token>' \
  -H 'Content-Type: application/json' \
  --data-binary @config-bundle.json
```

## 모드

- **`merge`**(기본) — 추가형. 생성·갱신만 하고 **삭제하지 않으며**, 역할의 기존 부여도 회수하지
  않습니다. 멱등: 같은 번들을 다시 적용해도 아무것도 안 바뀜.
- **`mirror`** — 대상을 *번들과 정확히 일치*시킵니다. merge에 더해 각 역할의 부여를 조정하고
  (번들에 없는 권한 회수), 번들에 없는 정의성 엔터티를 **삭제**합니다:
    - **메뉴**는 leaf부터 삭제(자식 먼저, 부모 나중);
    - **사용자에게 아직 배정된 역할은 건너뜀** — mirror는 사용자의 역할을 벗기지 않음;
    - **권한**은 테넌트의 역할들에서 회수한 뒤 삭제.

!!! danger "Mirror는 삭제한다"
    `mirror`는 무언가를 제거합니다. 적용 전 항상 **dry-run** diff를 검토하고, 배포당 단일
    테넌트 구성에서만 쓰세요(권한은 전역입니다).

## Dry-run 먼저

`dryRun=true`가 **기본**입니다. import는 전체 diff를 계산하고 아무것도 쓰지 않습니다. 결과는
섹션별(`permissions` / `roles` / `menus` / `users`)로 무엇이 다음이 될지 보고합니다:

- **created**, **updated**, **deleted**(mirror만), **skipped**(사용 중인 역할, 또는 기존
  사용자).

미리보기가 의도와 맞으면 `dryRun=false`로 실제 적용.

## 사용자 동기화 (opt-in)

`includeUsers=true`면:

- **export**는 사용자를 login id로 담습니다 — 이메일, 상태, 역할 코드 — 하지만 **비밀번호는
  절대** 안 담음.
- **import**는 **생성 전용**: 없는 사용자는 사용 불가 비밀번호 + `mustChangePassword`로
  생성된 뒤 역할이 코드로 배정됩니다. **기존 사용자는 절대 덮어쓰지 않음**(`skipped`로 보고).
  비밀번호는 이후 관리자 콘솔에서 설정.

사용자는 운영 데이터입니다; 새 환경에 계정을 심을 의도가 아니면 `includeUsers`는 꺼 두세요.

## 권장 워크플로

1. 로컬 DB에서 설정 설계(관리자 콘솔 또는 API).
2. 번들 **export**(정의성만이면 `includeUsers=false`).
3. 번들 JSON을 git에 커밋 — 이제 리뷰 가능·버전 관리되는 설정.
4. 대상에서 import **dry-run** 후 diff 검토.
5. 적용(`dryRun=false`). 추가/갱신은 `merge`; 대상을 번들과 정확히 일치시킬 의도일 때만 `mirror`.

## 관리자 콘솔

[관리자 콘솔](admin-console.md#config-sync)에는 전체 흐름을 다루는 **Config Sync** 페이지가
있습니다: export(보기 / 다운로드 / 복사), import(붙여넣기 또는 업로드 → dry-run diff → 적용),
`merge`/`mirror` 스위치, 사용자 동기화 토글.
