# 관리자 REST API

kit은 `/admin/api/v1` 아래에 관리 API를 노출합니다. 동반
[**devslab-kit-admin-ui**](https://github.com/devslab-kr/devslab-kit-admin-ui)가
이 위에 바로 올라가 있고, 직접 만든 도구에서 호출할 수도 있습니다.

| 리소스 | 역할 |
| --- | --- |
| `auth` | 로그인(JWT 반환) 및 비밀번호 변경. |
| `users` | 사용자 CRUD, 비밀번호 초기화, 잠금/해제, 상태 변경. |
| `roles` | 역할 CRUD 및 권한 할당. |
| `permissions` | 권한 정의 CRUD. |
| `groups` | 주체 그룹 CRUD 및 멤버십 관리. |
| `menus` | 메뉴 트리 관리(항목별 필요 권한, 순서). |
| `tenants` | 테넌트 CRUD 및 상태 변경. |
| `policies` | 등록된 ABAC 정책 목록 + `(subject, action, resource)` 튜플을 영속화 없이 **드라이런**. |
| `audit-logs` | 감사 추적 검색/필터. |
| `diagnostics` | 읽기 전용 프로브 — 로그인 테스트, 권한 확인, 메뉴 가시성 — 부작용 없음. |
| `settings` | 적용된 `devslab.kit.*` 설정의 실시간 읽기 전용 뷰(시크릿 마스킹). |
| `bootstrap/status` | 비인증 `GET`, `{ initialized: boolean }` 반환 — 최초 실행/설정 마법사의 분기점. |

## 인증

`bootstrap/status`를 제외한 모든 엔드포인트는 bearer 토큰이 필요합니다. `auth/login`에서
발급받으세요:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"…"}'
# → { "token": "<jwt>", "mustChangePassword": false, ... }
```

이후 호출에는 `Authorization: Bearer <jwt>`로 보냅니다. 토큰은 테넌트, 역할,
`mustChangePassword` 플래그를 담습니다([접근 제어](../guides/access.md) 참고).

## 인가 (Authorization)

인증만으로는 충분하지 않습니다. 모든 엔드포인트는 매핑된 `admin.*` 권한도
요구합니다. 읽기 엔드포인트는 해당 `*.read`, 변경 엔드포인트는 `*.write`를
요구합니다 — 예를 들어 `GET /users`는 `admin.user.read`, `POST /users`는
`admin.user.write`가 필요합니다. `auth/login`과 `bootstrap/status`는 공개이며,
`auth/change-password`는 유효한 토큰만 있으면 됩니다.

강제는 kit의 보안 체인에서 호출자의 **실효 권한**을 기준으로 이뤄집니다 — 토큰에서
읽는 게 아니라, 호출자가 가진 역할·그룹에서 매 요청마다 해석합니다([`PermissionChecker`](../guides/access.md)가
쓰는 것과 동일한 권한 해석). 따라서 권한 부여/회수가 호출자의 다음 요청에 즉시
반영되고 JWT는 작게 유지됩니다. 최초 관리자 부트스트랩이 모든 `admin.*` 권한을
`PLATFORM_ADMIN`에 시드하므로 시드된 관리자는 즉시 전체 API를 사용할 수 있습니다.

!!! tip "보안 커스터마이징"
    보안 체인과 JWT 필터는 모두 `@ConditionalOnMissingBean`이므로, 직접
    `SecurityFilterChain`이나 `JwtAuthenticationFilter`를 제공해 보호 방식을 바꿀 수
    있습니다. 직접 작성한 코드 안에서의 인가 검사는 kit의 `PermissionChecker`를
    주입해 사용하세요([접근 제어](../guides/access.md) 참고).

## 오류 (Errors)

모든 오류 응답은 RFC 7807 [`ProblemDetail`](https://www.rfc-editor.org/rfc/rfc7807)
(`application/problem+json`) 형식입니다:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": ["loginId: must not be blank"]
}
```

사람이 읽는 메시지는 `detail`(없으면 `title`)에서 읽으세요. 검증 실패에는 필드별 메시지 배열
`errors`가 추가됩니다. 흔한 상태 코드: `401`(자격 증명 오류), `403`(필요한 `admin.*` 권한 없음),
`400`(잘못된 입력), `409`(충돌).
