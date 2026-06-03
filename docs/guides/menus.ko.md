# 동적 메뉴

**동적 메뉴**는 사용자마다 자기 권한으로 볼 수 있는 항목만 보이는 내비게이션 트리입니다. 전체
메뉴를 **한 번만** 정의하고 항목마다 필요 권한을 붙여 두면, kit이 사용자마다 걸러진 사본을
내려줍니다. 프론트엔드 곳곳에 `v-if="canSeeX"`를 흩뿌릴 필요가 없습니다.

처음이면 [튜토리얼](../getting-started/tutorial.md)부터 — 이 가이드는 권한 몇 개가 정의된
실행 중인 앱이 있다고 가정합니다.

## 개념 잡기

```
한 번 정의 (테넌트 단위)              kit이 요청마다 필터링            화면 렌더링
─────────────────────────            ────────────────────            ──────────
Dashboard   (권한 없음)        ┐
Users       (user.read 필요)   ├──►  menusFor(현재 사용자)  ──►   MenuTree JSON
  └ Invite  (user.write 필요)  │     못 보는 항목 제거,             → 사이드바
Billing     (billing.read 필요)┘     빈 가지 정리
```

세 부분으로 나뉩니다:

1. **메뉴 항목**은 kit에 저장됩니다(한 줄에 라벨, 경로, 선택적 `requiredPermission`, 표시
   순서, 중첩용 선택적 부모).
2. **`MenuProvider`**가 특정 사용자 기준 트리를 만듭니다 — `requiredPermission`이 없는 사용자
   에게서 항목을 **빼고**, 그 결과 비어 버린 가지를 **정리**합니다.
3. **프론트엔드**는 걸러진 트리를 받아 그대로 그립니다. 가시성을 스스로 판단하지 않습니다.

## 1단계 — 메뉴 항목 정의

항목마다: `code`(고정 id), `label`(사용자가 보는 글자), `path`(링크 위치), 선택적 `icon`,
선택적 `requiredPermission`, `displayOrder`, 선택적 `parentId`(최상위면 생략).

=== "관리자 콘솔"

    1. [관리자 콘솔](admin-console.md) → **Menus** 열기.
    2. **New** 클릭 후 label / path / icon 입력.
    3. **Required permission**에서, 이 항목을 보려면 가져야 할 권한 선택(비우면 "모두에게").
    4. 중첩하려면 새 항목의 **Parent**를 기존 항목으로 설정.
    5. 드래그로 순서 변경 — `displayOrder`로 저장됩니다.

    전체 화면은 [관리자 콘솔 가이드 → Menus](admin-console.md#menus) 참고.

=== "REST API"

    ```bash
    # user.read 보유자에게만 보이는 최상위 "Users" 항목:
    curl -X POST localhost:8080/admin/api/v1/menus \
      -H 'Authorization: Bearer <token>' \
      -H 'Content-Type: application/json' \
      -d '{
            "tenantId": "default",
            "code": "users",
            "label": "Users",
            "path": "/users",
            "icon": "pi pi-users",
            "requiredPermission": "user.read",
            "displayOrder": 20,
            "parentId": null
          }'
    ```

    `requiredPermission`과 `parentId`는 선택 — 공개·최상위 항목이면 생략(또는 `null`).
    `menus` 리소스 전체(목록, 트리, 수정, 삭제)는 [관리자 REST API](../reference/admin-api.md) 참고.

## 2단계 — 걸러진 트리를 프론트엔드에 내려주기

현재 사용자의 트리를 돌려주는 엔드포인트 하나를 두세요. 필터링은 kit이 하니, `menusFor`가
주는 걸 그대로 반환하면 됩니다:

```java
// src/main/java/com/example/myapp/NavController.java
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.MenuTree;
import kr.devslab.kit.identity.CurrentUserProvider;

@RestController
class NavController {

    private final MenuProvider menus;
    private final CurrentUserProvider users;

    NavController(MenuProvider menus, CurrentUserProvider users) {
        this.menus = menus;
        this.users = users;
    }

    @GetMapping("/api/nav")
    MenuTree nav() {
        return menus.menusFor(users.current().orElseThrow());
    }
}
```

응답은 `MenuTree` — 루트 `MenuItem` 목록이고, 각 항목의 허용된 자식이 `children` 아래에
중첩됩니다:

```json
{
  "roots": [
    { "code": "dashboard", "label": "Dashboard", "path": "/", "icon": "pi pi-home",
      "requiredPermission": null, "children": [] },
    { "code": "users", "label": "Users", "path": "/users", "icon": "pi pi-users",
      "requiredPermission": "user.read", "children": [
        { "code": "users.invite", "label": "Invite", "path": "/users/invite",
          "icon": "pi pi-user-plus", "requiredPermission": "user.write", "children": [] }
      ] }
  ]
}
```

`user.read`가 **없는** 사용자에게는 `users` 노드 자체가 안 보이고 — 유일한 자식도 같이
사라지므로 — 덩그러니 남는 게 없습니다.

## 3단계 — 화면에 그리기

프론트엔드는 트리를 그대로 렌더링합니다. 최소 예:

```vue
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const roots = ref([])
onMounted(async () => { roots.value = (await axios.get('/api/nav')).data.roots })
</script>

<template>
  <nav>
    <RouterLink v-for="item in roots" :key="item.code" :to="item.path">
      <i :class="item.icon" /> {{ item.label }}
      <!-- 중첩 메뉴는 item.children 재귀 -->
    </RouterLink>
  </nav>
</template>
```

## 캐싱

사용자별 트리는 공유 [캐시](cache.md)에 (user id 키로) 캐시되어, 반복되는 내비게이션 요청
마다 다시 계산하지 않습니다. 사용자가 볼 수 있는 메뉴를 수정하면 해당 항목이 자동으로
무효화됩니다 — 직접 관리할 일이 없습니다.

!!! note "의존 방향"
    메뉴는 권한을 참조하지만, 권한은 메뉴를 전혀 모릅니다 — 의존이 거꾸로 뒤집히지 않습니다
    (핵심 [설계 원칙](../index.md#why-a-starter)).

## 더 보기

- [Access (RBAC + ABAC)](access.md) — 항목에 붙일 권한을 정의.
- [관리자 콘솔 → Menus](admin-console.md#menus) — 트리 편집기.
- [관리자 REST API](../reference/admin-api.md) — `menus` 리소스.
