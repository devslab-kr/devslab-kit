# Config Sync

Promote **definitional platform config** — permissions, roles (and the permission codes
they grant) and menus — from one environment to another as a portable, code-keyed bundle,
instead of hand-editing each target database.

The admin console can manage the whole RBAC graph at runtime, but that config lives in the
**database**, not in code. A team designs it locally, then needs the *same* structure in
dev / staging / production — each with its own database. Config sync is the first-class way
to move it. See [ADR 0003](../adr/0003-config-sync.md) for the rationale and design.

!!! warning "Off by default, and never in production"
    Config sync is a **dev/staging convenience**, disabled unless you opt in, and it
    **refuses to start** under a `prod` / `production` profile. Production config is promoted
    by applying the git-committed bundle on deploy — not by an ad-hoc push to a live system.

## Enable it

```yaml
devslab:
  kit:
    config-sync:
      enabled: true   # default false — the whole surface (endpoints + UI) is inert otherwise
```

If `enabled=true` while a `prod`/`production` profile is active, the application fails fast
at startup with a clear message rather than silently disabling the feature.

## What's in the bundle

| Included (definitional) | Excluded |
| --- | --- |
| Permissions (`code`, description) | Audit logs (history) |
| Roles (`code`, name, **permission codes**) | ABAC policies (these are *code*, not data) |
| Menus (`code`, parent code, label, path, icon, required permission code, order) | Passwords / secrets |
| Users — **opt-in only**, by login id, with role codes and **no password** | |

Everything is keyed by **natural codes**, never database UUIDs, so a bundle exported from one
environment applies cleanly to another whose ids differ.

## Endpoints

| | |
| --- | --- |
| `GET /admin/api/v1/config/export?tenantId={t}&includeUsers=false` | Returns the bundle as JSON. |
| `POST /admin/api/v1/config/import?mode=merge&dryRun=true&includeUsers=false` | Applies a bundle; returns a per-section diff. |

## Modes

- **`merge`** (default) — additive. Creates and updates; **never deletes**, and never revokes
  a role's existing grants. Idempotent: re-applying the same bundle changes nothing.
- **`mirror`** — makes the target *match the bundle exactly*. On top of the merge it reconciles
  each role's grants (revoking permissions the bundle omits) and **deletes** definitional
  entities absent from the bundle:
    - **menus** are deleted leaf-first (a child before its parent);
    - a **role still assigned to a user is skipped** — mirror never strips a user's role;
    - **permissions** are revoked from the tenant's roles, then deleted.

!!! danger "Mirror deletes"
    `mirror` removes things. Always review the **dry-run** diff before applying, and prefer it
    only for single-tenant-per-deployment setups (permissions are global).

## Dry-run first

`dryRun=true` is the **default**. The import computes the full diff and writes nothing. The
result reports, per section (`permissions` / `roles` / `menus` / `users`), what would be:

- **created**, **updated**, **deleted** (mirror only), and **skipped** (a role in use, or an
  existing user).

Apply for real with `dryRun=false` once the preview matches your intent.

## User sync (opt-in)

With `includeUsers=true`:

- **export** carries users by login id — email, status and role codes — but **never a
  password**.
- **import** is **create-only**: a missing user is created with no usable password and
  `mustChangePassword`, then assigned its roles by code. An **existing user is never
  overwritten** (it is reported as `skipped`). Set the password via the admin console afterwards.

Users are operational data; leave `includeUsers` off unless you specifically want to seed
accounts into a fresh environment.

## Recommended workflow

1. Design config locally against your local database (admin console or API).
2. **Export** the bundle (`includeUsers=false` for definitional-only).
3. Commit the bundle JSON to git — it is now reviewable, versioned config.
4. On the target, **dry-run** the import and review the diff.
5. Apply (`dryRun=false`). Use `merge` to add/update; `mirror` only when you intend the target
   to match the bundle exactly.

## Admin console

The [admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) has a **Config Sync**
page that drives the whole flow: export (view / download / copy), import (paste or upload →
dry-run diff → apply), the `merge`/`mirror` switch, and the user-sync toggle.
