# 최초 관리자 부트스트랩

빈 데이터베이스에는 사용자가 없습니다 — 그럼 영구 백도어 없이 처음에 어떻게 로그인할까요?
kit의 **최초 관리자 부트스트랩**이 첫 부팅 시 사용 가능한 관리자를 프로비저닝합니다.
opt-in이며 프로퍼티 기반입니다(배경: [ADR 0001](../adr/0001-bootstrap-admin.md)).

## 하는 일

`bootstrap.enabled = true`이면 시작 시 kit이 (없을 때만) **멱등하게** 생성합니다:

1. 테넌트 `bootstrap.tenant-id`,
2. 전체 `admin.*` 권한 세트를 가진 `PLATFORM_ADMIN` 역할,
3. 그 테넌트에 그 역할을 가진 관리자 사용자(`bootstrap.admin-login-id`).

멱등이므로 켜둔 채로 둬도 안전합니다 — 이후 부팅은 레코드를 찾고 아무것도 하지 않습니다.

## 설정

```yaml
devslab:
  kit:
    bootstrap:
      enabled: true
      tenant-id: default
      admin-login-id: admin
      admin-password: ${DEVSLAB_BOOTSTRAP_ADMIN_PASSWORD:}   # 비우면 랜덤, 한 번 로깅
      must-change-password: true
```

모든 키는 [설정 레퍼런스](../reference/configuration.md)를 참고하세요.

## 비밀번호

- 알려진 값이 필요하면 **명시적으로 설정**(예: 로컬 개발 `admin`/`admin`).
- **비우면** kit이 강력한 랜덤 비밀번호를 생성해 시작 시 **한 번** 로깅합니다 —
  로그에서 복사하면 이후엔 사라집니다.
- `prod` / `production` 프로파일에서는 약한 부트스트랩 비밀번호로 **시작을 거부**하므로,
  placeholder가 운영에 새지 않습니다.

알려진 비밀번호는 `must-change-password: true`와 함께 써서 운영자가 첫 로그인 시
교체하게 하세요.

## 최초 실행 감지

비인증 엔드포인트 `GET /admin/api/v1/bootstrap/status`가 `{ "initialized": boolean }`을
반환합니다. 설정 마법사나 랜딩 페이지가 이를 기준으로 분기할 수 있습니다 — 예: 갓 배포된
환경을 로그인 폼 대신 "관리자 생성" 플로우로 보냄.

## 운영 가이드

실제 환경에서는 다음 중 하나를 권장합니다:

- 강력한 `admin-password`(시크릿으로 주입) + `must-change-password: true`, 또는
- `enabled: false`로 두고 최초 관리자를 out-of-band(SQL/마이그레이션/운영 도구)로 프로비저닝.
