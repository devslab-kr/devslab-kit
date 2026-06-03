# 최초 관리자 부트스트랩

빈 데이터베이스에는 사용자가 없습니다 — 그럼 영구 백도어 없이 처음에 어떻게 로그인할까요?
kit의 **최초 관리자 부트스트랩**이 첫 부팅 때 쓸 수 있는 관리자를 만들어 줍니다: opt-in,
속성 기반, 멱등(idempotent)입니다(배경: [ADR 0001](../adr/0001-bootstrap-admin.md)).

처음이면 [튜토리얼](../getting-started/tutorial.md)이 바로 이걸로 로그인까지 데려갑니다 — 이
가이드는 그 단계의 레퍼런스입니다.

## 무엇을 하나

`bootstrap.enabled = true`면, 시작 시 kit이 **멱등하게** (없을 때만) 생성합니다:

1. 테넌트 `bootstrap.tenant-id`,
2. 전체 `admin.*` 권한을 가진 `PLATFORM_ADMIN` 역할,
3. 그 역할을 가진 관리자 사용자(`bootstrap.admin-login-id`)를 해당 테넌트에.

멱등이므로 켜 둬도 안전합니다 — 이후 부팅은 레코드를 찾고 아무것도 하지 않습니다.

## 설정

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    bootstrap:
      enabled: true
      tenant-id: default
      admin-login-id: admin
      admin-password: ${DEVSLAB_BOOTSTRAP_ADMIN_PASSWORD:}   # 비우면 → 랜덤, 한 번만 로그
      must-change-password: true
```

모든 키는 [설정 레퍼런스](../reference/configuration.md#bootstrap) 참고.

## 비밀번호

- 알려진 값이 필요하면 **명시 설정**(예: 로컬 개발 `admin`/`admin`).
- **비워 두면** kit이 강한 랜덤 비밀번호를 생성해 시작 시 **한 번만** 로그에 찍습니다 —
  로그에서 복사하면 그 뒤엔 사라집니다.
- `prod` / `production` 프로파일에서는 약한 부트스트랩 비밀번호로 **시작을 거부**하므로,
  플레이스홀더가 운영에 새어 들어가지 않습니다.

알려진 비밀번호는 `must-change-password: true`와 함께 써서 운영자가 첫 로그인 때 교체하게
하세요.

## 처음 로그인하기

위 설정(`admin`/`admin`)으로 앱을 시작한 뒤:

=== "관리자 콘솔"

    1. [관리자 콘솔](admin-console.md)을 띄우고 브라우저에서 엽니다.
    2. `admin` / `admin`(테넌트 `default`)으로 로그인.
    3. 이제 `PLATFORM_ADMIN`을 가졌으니 모든 화면을 쓸 수 있습니다.

=== "REST API"

    ```bash
    # 자격 증명을 JWT로 교환:
    curl -X POST localhost:8080/admin/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","loginId":"admin","rawPassword":"admin"}'
    # → { "token": "eyJ…" }  — `Authorization: Bearer eyJ…`로 전송
    ```

## 첫 실행 감지

인증 없이 호출하는 `GET /admin/api/v1/bootstrap/status`는 `{ "initialized": boolean }`을
반환합니다. 셋업 마법사나 랜딩 페이지가 이걸로 분기할 수 있습니다 — 예: 갓 배포된 인스턴스를
로그인 폼 대신 "관리자 만들기" 흐름으로 보냅니다:

```bash
curl localhost:8080/admin/api/v1/bootstrap/status
# → { "initialized": false }   (아직 관리자 없음)  /  true (프로비저닝됨)
```

일부러 공개입니다 — 마법사는 누가 인증하기 *전에* 이걸 호출합니다.

## 운영 가이드

실제 환경에서는 둘 중 하나를 권장합니다:

- 강한 `admin-password`(시크릿으로 주입) + `must-change-password: true`, 또는
- `enabled: false`로 두고 최초 관리자를 외부에서 프로비저닝(SQL/마이그레이션/운영 도구).

## 더 보기

- [튜토리얼](../getting-started/tutorial.md) — 실행 중인 앱에서의 부트스트랩.
- [Access (RBAC + ABAC)](access.md) — `PLATFORM_ADMIN`과 `admin.*`이 부여하는 것.
- [설정 레퍼런스](../reference/configuration.md#bootstrap) — 모든 키.
