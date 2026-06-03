# 접근 제어 (RBAC + ABAC)

**인가(authorization)**는 누가 무엇을 할 수 있는지 정합니다. `devslab-kit`은 두 계층으로
처리합니다:

1. **RBAC**(역할 기반) — 거친 계층. **사용자**가 **역할**을 가지고(직접 또는 **그룹**을 통해),
   역할이 **권한**을 부여합니다 — `admin.user.read` 같은 고정 문자열 코드. "이 사용자가 X를
   할 수 있나?"
2. **ABAC**(속성 기반) — RBAC 결정을 속성으로 더 세밀하게 다듬는 선택 계층. "그것도 *이 특정
   리소스에, 지금* 할 수 있나?"

대부분의 앱은 RBAC만으로 충분합니다. 권한이 *데이터*에 좌우될 때(소유자만 수정, 같은 테넌트만,
영업시간) ABAC를 씁니다. 처음이면 [튜토리얼](../getting-started/tutorial.md)부터 — 6~9단계가
이걸 실제로 설정합니다.

## 개념 잡기

```
            ┌─ 직접 역할 ──┐
   사용자 ──┤              ├──► 역할 ──► 권한          ← RBAC: 권한을 가졌나?
            └─ 그룹 ─ 역할 ┘
                                      그다음, 선택적으로:
   권한 + 리소스 속성 ──► Policy ──► PERMIT/DENY        ← ABAC: 이 리소스에 대해?
```

사용자의 **유효 권한**은 직접 역할과 그룹 역할이 가진 권한의 합집합입니다.

## 1단계 — 역할·권한 설정

권한을 정의하고, 역할로 묶고, 역할을 사용자에게 배정합니다. (최초 관리자
[부트스트랩](bootstrap.md)이 이미 전체 `admin.*`을 가진 `PLATFORM_ADMIN`을 심어 둡니다 — 여기서는
직접 추가하는 법입니다.)

=== "관리자 콘솔"

    [관리자 콘솔](admin-console.md)에서:

    1. **Permissions** → **New** → `doc.read` 같은 코드(+설명) 추가.
    2. **Roles** → **New** → 예: `editor` 생성.
    3. 역할 열기 → `doc.read`(외 필요한 것)를 **grant**.
    4. **Users** → 사용자 선택 → `editor` 역할 **assign**(또는 그 역할을 가진 **그룹**에 추가).

=== "REST API"

    ```bash
    # 1. 권한 생성
    curl -X POST localhost:8080/admin/api/v1/permissions \
      -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
      -d '{"code":"doc.read","description":"Read documents"}'

    # 2. 역할 생성
    curl -X POST localhost:8080/admin/api/v1/roles \
      -H 'Authorization: Bearer <token>' -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","code":"editor","name":"Editor"}'

    # 3. 역할에 권한 부여   (id는 위 응답에서)
    curl -X POST localhost:8080/admin/api/v1/roles/{roleId}/permissions/{permissionId} \
      -H 'Authorization: Bearer <token>'

    # 4. 사용자에게 역할 배정
    curl -X POST localhost:8080/admin/api/v1/roles/{roleId}/users/{userId} \
      -H 'Authorization: Bearer <token>'
    ```

    `permissions`·`roles`·`groups` 리소스 전체는 [관리자 REST API](../reference/admin-api.md) 참고.

## 2단계 — 코드에서 권한 확인

`PermissionChecker`를 주입합니다. 현재 사용자 기준으로 평가합니다:

```java
// src/main/java/com/example/myapp/DocService.java
import kr.devslab.kit.access.PermissionChecker;
import kr.devslab.kit.access.Permission;

@Service
class DocService {

    private final PermissionChecker access;

    DocService(PermissionChecker access) { this.access = access; }

    Document open(String docId) {
        access.check(Permission.of("doc.read"));   // 없으면 PermissionDeniedException
        return load(docId);
    }
}
```

`hasPermission(Permission)`, `hasAnyPermission(Permission...)`,
`hasAllPermissions(Permission...)`도 있습니다 — 예외 대신 분기하고 싶을 때 사용.

## 그룹

**그룹**은 사용자 집합을 위해 역할을 묶습니다 — 역할을 하나하나 붙이는 대신 사용자를
`eng-team`에 한 번 넣으면 됩니다. 그룹(멤버 + 역할 부여)은 [관리자 콘솔](admin-console.md)이나
`groups` REST 리소스에서 관리합니다. 사용자의 유효 권한에는 그룹의 역할이 자동 포함됩니다.

## 3단계 — 리소스 단위 규칙을 위한 ABAC { #abac-policies }

RBAC는 "이 사용자가 권한을 가졌는가?"에 답합니다. ABAC는 더 세밀한 "…*이 특정 리소스에 대해,
지금?*"에 답합니다. 하나 이상의 **`Policy`** 빈을 구현하면, kit의 `DefaultPolicyEvaluator`가 모든
`Policy` 빈을 모아 `name()`으로 디스패치합니다. (해당 이름의 정책이 없으면 평가는
`NOT_APPLICABLE`을 반환합니다.)

```java
// src/main/java/com/example/myapp/DocOwnerPolicy.java
import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import org.springframework.stereotype.Component;

@Component
class DocOwnerPolicy implements Policy {

    @Override public String name() { return "doc-owner"; }

    @Override
    public PolicyDecision evaluate(PolicyContext ctx) {
        // ctx 제공: userId(), tenantId(), resourceType(), resourceId(),
        // resourceAttributes(), environmentAttributes()
        Object owner = ctx.resourceAttributes().get("ownerLoginId");
        return owner != null /* && owner가 현재 사용자와 같으면 */
                ? PolicyDecision.PERMIT
                : PolicyDecision.DENY;
    }
}
```

그런 다음 `check`의 ABAC 오버로드로, 빌더로 컨텍스트를 만들어 게이트합니다:

```java
// DocService 안, 특정 문서 편집 시:
PolicyContext ctx = PolicyContext.builder()
        .user(userId)
        .tenant(tenantId)
        .resource("doc", docId)
        .resourceAttributes(Map.of("ownerLoginId", doc.ownerLoginId()))
        .build();

access.check(Permission.of("doc.read"), "doc-owner", ctx);   // RBAC 먼저, 그다음 정책
```

`check`는 RBAC **와** 명명된 정책을 모두 강제합니다: 사용자는 `doc.read`를 가져야 *하고*
`doc-owner` 정책이 `PERMIT`해야 합니다.

더 풍부한 답(이유 + 어떤 규칙이 매칭됐는지, 테스트 엔드포인트가 노출)을 원하면
`evaluateDetailed(PolicyContext)`를 오버라이드해 `PolicyEvaluation`을 반환하세요 — 예:
`PolicyEvaluation.deny("not the owner", List.of("ownership"))`.

!!! tip "결정 dry-run"
    관리자 콘솔의 **Policies** 화면(및 `policies` 관리 엔드포인트)은 `(subject, action, resource)`
    튜플을 **부작용 없이** 평가할 수 있어, 경로에 엮기 전에 정책을 테스트할 수 있습니다.
    [관리자 콘솔 가이드](admin-console.md)와 [관리자 REST API](../reference/admin-api.md) 참고.

## 더 보기

- [관리자 콘솔](admin-console.md) — 역할·권한·그룹 관리 및 정책 테스트.
- [동적 메뉴](menus.md) — 사용자에게 권한이 허용하는 메뉴만 표시.
- [설정 레퍼런스](../reference/configuration.md#identity) — JWT + 잠금 설정.
