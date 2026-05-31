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

!!! tip "API 보호"
    엔드포인트는 Spring Security와 kit의 `PermissionChecker`(`admin.*` 권한)로
    보호됩니다. 모든 빈이 `@ConditionalOnMissingBean`이므로, 직접 시큐리티 체인이나
    `PermissionChecker`를 제공해 보호 방식을 바꿀 수 있습니다.
