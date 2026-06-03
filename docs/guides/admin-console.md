# Admin console — using every screen

The [admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) is a ready-made
web UI for everything the kit manages. This guide walks through **every menu**, step by
step. It pairs with the kit's REST API — anything here can also be done with the
[Admin REST API](../reference/admin-api.md).

!!! info "Open the console"
    Run the admin-ui (`npm run dev` → `http://localhost:5173`); in dev it proxies
    `/admin/api` to your app on `:8080`. Sign in with your platform account (e.g. the
    bootstrap `admin`). The left sidebar groups the screens into **Identity & Access**,
    **Platform**, and **Observability**. Each row's actions are the small icon buttons on
    the right — **hover any icon for a tooltip**.

---

## Identity & Access

### Users
Platform accounts that can sign in and be granted access.

- **Create** — top-right **Create**: login id, email (optional), password, provider (default `LOCAL`).
- **Per-row actions** (hover for labels):
    - **Lock / Unlock** (padlock) — block or restore sign-in.
    - **Reset password** (key).
    - **Manage roles** (id-card) — opens a two-pane picker (*Available* | *Assigned*); move roles across and **Save**. Roles assigned here apply directly to the user.
    - **Manage groups** (people) — same picker for group membership.
    - **Change status** (pencil) — `ACTIVE` / `LOCKED` / `DISABLED` / `PENDING_VERIFICATION`.
    - **Delete** (trash).

### Roles
A named bundle of permissions.

- **Create** — login id… **Create**: `code` (e.g. `LIBRARIAN`) + display name.
- **Per-row**: **Manage permissions** (key) → the *Available | Assigned* picker of permission codes → **Save**; **Rename** (pencil); **Delete** (trash).
- A user's effective permissions = the union of their direct roles + their groups' roles.

### Permissions
Fine-grained capabilities, coded as `resource.action` (e.g. `book.read`).

- **Create** — you don't type the whole string: pick/type a **resource** (autocompletes from existing namespaces), pick an **action** (`read`/`write`/`delete`/`manage`/…, or type your own). The composed code (`book.read`) previews live. Add an optional description → **Create**.
- **Edit** the description, or **Delete**. A new resource is created simply by typing a new name when you create the first permission under it.

### Groups
Collections of users that share roles — assign once instead of per-user.

- **Create** — `code` + name.
- **Per-row**: **Members** (people) → pick users; **Roles** (key) → pick roles that flow to all members; **Rename**; **Delete**.

---

## Platform

### Menus
A permission-gated navigation tree your product UI can render per user.

- Shown as a **tree**. **Create** a root item, or use a node's **add-child** action.
- Each item has a label, path, icon, **required permission** (a dropdown of existing permission codes — only users holding it see the item), and display order. **Edit** / **Delete** per node.
- The kit serves the filtered tree to a signed-in user; your front end decides how to render it (see [Menus guide](menus.md)).

### Tenants
Isolated workspaces; all platform data is scoped to a tenant.

- **Create** a tenant (`code` + name). **Change status** (`ACTIVE` / `SUSPENDED` / `ARCHIVED`). **Delete**.
- In `single`-tenant mode you typically only have `default`.

### Policies (ABAC)
Attribute-based rules layered on top of roles. **Policies are code** (you implement
`Policy` beans — see [Access guide](access.md#abac-policies)); this screen **lists** the
registered policies and lets you **test** them.

- Pick a policy, fill in a **subject** (user/tenant), a **resource** (type, id, attributes) and any **environment** attributes, then **Test**.
- The result is the **decision** (`PERMIT` / `DENY` / `NOT_APPLICABLE`) with a **reason** and the **matched rules** — a dry-run with no side effects.

### Settings
A live, read-only view of the kit's **effective configuration** (`devslab.kit.*`) —
JWT, tenant, identity lockout, audit, cache. Secrets are masked. Use it to confirm what
the running app actually loaded. (To change values, edit your `application.yml` — see
[Configuration](../reference/configuration.md).)

### Config Sync
Promote definitional config (permissions, roles, menus) between environments. **Off by
default** and refused under a production profile (see [Config Sync guide](config-sync.md)).

- **Export** — snapshot this environment as a code-keyed JSON bundle: view / **Download** / **Copy**. Toggle **Include users** to add users (no passwords).
- **Import** — paste or upload a bundle, choose **merge** (add/update) or **mirror** (also delete extras), optionally **Sync users**, then **Preview (dry-run)** to see the per-section diff (created / updated / deleted / skipped). **Apply** unlocks only after a dry-run of the exact bundle.

---

## Observability

### Dashboard
Landing page: KPI cards (users, tenants, current tenant, signed-in user) and the most
recent audit events. **Refresh** re-pulls.

### Diagnostics
Probe authorization for any user without impersonating them:

- **Login test** — verify a tenant/login/password combination.
- **Permission check** — pick a **user** and a **permission** (dropdowns, no UUID typing) → allowed or not, and why.
- **Menu visibility** — pick a user → the menu **tree** they would see (permission-filtered).

### Audit Logs
Every administrative action, recorded asynchronously.

- **Filter** by tenant, actor, action, target type, outcome, and time range; results are **lazily paginated**.
- Click a row to inspect its **JSON payload** (the before/after metadata).

---

## See also

- [Tutorial: from zero to a running app](../getting-started/tutorial.md) — does much of the above via API + console.
- [Access (RBAC + ABAC)](access.md) · [Menus](menus.md) · [Config Sync](config-sync.md)
- [Admin REST API](../reference/admin-api.md) — the endpoints behind every screen.
