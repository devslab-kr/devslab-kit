# Dynamic Menus

A **dynamic menu** is a navigation tree where each user sees only the items their
permissions allow. You define the full menu **once**, tag each item with a required
permission, and the kit hands every user their own filtered copy. No `v-if="canSeeX"`
scattered across your frontend.

New here? Do the [Tutorial](../getting-started/tutorial.md) first, then come back —
this guide assumes you have a running app with a few permissions defined.

## The mental model

```
You define (once, per tenant)        Kit filters (per request)      You render
─────────────────────────────        ─────────────────────────      ──────────
Dashboard   (no permission)     ┐
Users       (needs user.read)   ├──►  menusFor(currentUser)  ──►   MenuTree JSON
  └ Invite  (needs user.write)  │     drops items the user           → your sidebar
Billing     (needs billing.read)┘     can't see, prunes empties
```

Three moving parts:

1. **Menu items** live in the kit (one row each: a label, a path, an optional
   `requiredPermission`, a display order, an optional parent for nesting).
2. **`MenuProvider`** builds the tree for a given user — **dropping** items whose
   `requiredPermission` the user lacks and **pruning** branches that end up empty.
3. **Your frontend** fetches the filtered tree and renders it. It never decides
   visibility itself.

## Step 1 — Define your menu items

Each item has: a `code` (stable id), a `label` (what users see), a `path` (where it
links), an optional `icon`, an optional `requiredPermission`, a `displayOrder`, and an
optional `parentId` (omit for a top-level item).

=== "Admin console"

    1. Open the [admin console](admin-console.md) → **Menus**.
    2. Click **New** and fill in label / path / icon.
    3. In **Required permission**, pick the permission a user must hold to see this
       item (leave blank for "everyone").
    4. To nest, set the new item's **Parent** to an existing item.
    5. Drag to reorder — the order is saved as `displayOrder`.

    See the [Admin Console guide → Menus](admin-console.md#menus) for the full screen.

=== "REST API"

    ```bash
    # Top-level "Users" item, visible only to holders of user.read:
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

    `requiredPermission` and `parentId` are optional — omit (or `null`) for a
    public, top-level item. See the [Admin REST API](../reference/admin-api.md) for
    the full `menus` resource (list, tree, update, delete).

## Step 2 — Serve the filtered tree to your frontend

Expose one endpoint that returns the current user's tree. The kit does the filtering;
you just return what `menusFor` gives you:

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

The response is a `MenuTree` — a list of root `MenuItem`s, each with its allowed
children nested under `children`:

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

A user **without** `user.read` simply won't see the `users` node at all — and because
its only child is then gone too, nothing dangling is left behind.

## Step 3 — Render it

Your frontend renders the tree verbatim. A minimal example:

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
      <!-- recurse into item.children for nested menus -->
    </RouterLink>
  </nav>
</template>
```

## Caching

The per-user tree is cached on the shared [cache](cache.md) (keyed by user id), so
repeated navigation requests don't recompute it. Editing a user's visible menus
evicts their entry automatically — you don't manage this.

!!! note "Direction of dependency"
    Menus may reference permissions, but permissions know nothing about menus — the
    dependency never reverses (a core [design principle](../index.md#why-a-starter)).

## See also

- [Access (RBAC + ABAC)](access.md) — define the permissions you tag items with.
- [Admin Console → Menus](admin-console.md#menus) — the tree editor.
- [Admin REST API](../reference/admin-api.md) — the `menus` resource.
