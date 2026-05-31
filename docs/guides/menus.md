# Dynamic Menus

The kit builds a **per-user menu tree**: you define the full menu once (with a
required permission per item), and each user sees only the items they're allowed.

## How it works

1. Menu items are stored with an optional `requiredPermission` and an order.
2. `MenuProvider` builds the tree for a given user, **filtering out** items whose
   required permission the user lacks (and pruning now-empty branches).
3. The result is an immutable `MenuTree(List<MenuItem> roots)`.

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

## Managing items

Create, edit, reorder and delete menu items through the admin API (`menus`) — see
[Admin REST API](../reference/admin-api.md). The
[admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) provides a tree
editor for them.

## Caching

The per-user tree is cached on the shared [cache](cache.md) (keyed by user id), so
repeated navigation requests don't recompute it. Editing a user's visible menus
evicts their entry.

!!! note "Direction of dependency"
    Menus may reference permissions, but permissions know nothing about menus — the
    dependency never reverses (a core [design principle](../index.md#why-a-starter)).
