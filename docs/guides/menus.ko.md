# 동적 메뉴

kit은 **사용자별 메뉴 트리**를 만듭니다: 전체 메뉴를 한 번 정의하면(항목마다 필요 권한
지정), 각 사용자는 허용된 항목만 보게 됩니다.

## 동작 방식

1. 메뉴 항목은 선택적 `requiredPermission`과 순서와 함께 저장됩니다.
2. `MenuProvider`가 주어진 사용자에 대해 트리를 만들면서, 사용자가 갖지 못한 필요 권한의
   항목을 **걸러냅니다**(그리고 비게 된 가지를 잘라냅니다).
3. 결과는 불변 `MenuTree(List<MenuItem> roots)`입니다.

```java
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.MenuTree;

@RestController
class NavController {
    private final MenuProvider menus;
    private final CurrentUserProvider users;

    NavController(MenuProvider menus, CurrentUserProvider users) {
        this.menus = menus; this.users = users;
    }

    @GetMapping("/api/nav")
    MenuTree nav() {
        return menus.menusFor(users.current().orElseThrow());
    }
}
```

## 항목 관리

메뉴 항목의 생성·수정·순서변경·삭제는 관리자 API(`menus`)로 합니다 —
[관리자 REST API](../reference/admin-api.md) 참고.
[관리자 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)이 트리 편집기를 제공합니다.

## 캐싱

사용자별 트리는 공유 [캐시](cache.md)에 (사용자 id를 키로) 캐시되어, 반복되는 내비게이션
요청이 매번 재계산하지 않습니다. 사용자의 노출 메뉴를 수정하면 해당 항목이 evict됩니다.

!!! note "의존 방향"
    메뉴는 권한을 참조할 수 있지만, 권한은 메뉴를 전혀 모릅니다 — 그 반대 방향 의존은
    없습니다(핵심 [설계 원칙](../index.md#why-a-starter)).
