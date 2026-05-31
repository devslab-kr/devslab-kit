# 접근 제어 (RBAC + ABAC)

`devslab-kit`의 인가는 두 계층입니다:

1. **RBAC** — 사용자는 **역할**을 가지며(직접 또는 **그룹**을 통해), 역할은 **권한**을
   부여합니다(`admin.user.read` 같은 안정적인 문자열 코드).
2. **ABAC** — RBAC 결정을 속성(주체·행위·리소스·환경)으로 더 정밀하게 다듬는 선택적
   **정책** 계층.

## 권한 확인

`PermissionChecker`를 주입하세요. 현재 사용자를 기준으로 평가합니다:

```java
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.Permission;

@Service
class UserAdminService {
    private final PermissionChecker access;

    UserAdminService(PermissionChecker access) { this.access = access; }

    void deactivate(String loginId) {
        access.check(Permission.of("admin.user.write"));   // 없으면 PermissionDeniedException
        // …
    }
}
```

이 외에 `hasPermission(Permission)`, `hasAnyPermission(Permission...)`,
`hasAllPermissions(Permission...)`도 있습니다.

## 그룹

**그룹**은 여러 사용자를 위해 역할을 묶습니다 — 역할을 일일이 붙이는 대신 사용자를
`eng-team`에 한 번 넣으면 됩니다. 사용자의 유효 권한은 직접 역할과 그룹 역할의 합집합입니다.

## ABAC 정책

RBAC는 "이 사용자가 권한을 가졌는가?"에 답합니다. ABAC는 더 세밀한 "…*이 특정 리소스에
대해, 지금?*"에 답합니다. `PolicyEvaluator` SPI를 구현하면(기본은 모두 허용) kit이
이를 참조합니다:

```java
import kr.devslab.kit.access.policy.*;

@Bean
PolicyEvaluator policyEvaluator() {
    return new PolicyEvaluator() {
        @Override public PolicyDecision evaluate(PolicyContext ctx) {
            // ctx.action(), ctx.resourceType(), ctx.subjectAttributes(),
            // ctx.environment() … 을 보고 permit / deny PolicyDecision 반환
            ...
        }
        @Override public List<Policy> policies() { return List.of(); }
    };
}
```

그런 다음 ABAC 인지 오버로드로 게이트합니다:

```java
access.check(Permission.of("doc.read"), "doc-owner-policy",
    new PolicyContext(userId, tenantId, Map.of(), "read", "doc", docId, Map.of(), Map.of()));
```

부작용 없이 `(subject, action, resource)` 튜플을 드라이런하려면 관리자 API의 `policies`
엔드포인트를 쓰세요 — [관리자 REST API](../reference/admin-api.md) 참고.

관련 JWT·잠금 설정은 [설정 레퍼런스](../reference/configuration.md#identity)를 참고하세요.
