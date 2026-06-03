# ADR 0003 — Platform config sync across environments (export/import, not a live push)

- **Status:** Proposed
- **Date:** 2026-06-03
- **Implemented by (proposed):** `devslab-kit-admin-api` config endpoints
  (`config/export`, `config/import`), an export/import service in `-core`, gating in
  `devslab-kit-autoconfigure` (`devslab.kit.config-sync.*`), and a "Config Sync" page in
  `devslab-kit-admin-ui`. Targets `0.4.0`. See the PR breakdown below.

## Context

After ADR 0001/0002 and the 0.3.x line, the admin console can manage the whole RBAC
graph at runtime — permissions, roles, role→permission mappings, groups, and menus. All
of that lives in the **database**, written through the admin API.

The problem this raises: that config is **DB state, not code**. A team designs it
locally (against a local database), then needs the *same* structural config in dev /
staging / production — each of which has its own database. There is no first-class way to
promote it. The only reproducible path today is a hand-written seeder
(an `ApplicationRunner` in the consumer that idempotently upserts the rows), which works
but is bespoke per consumer and easy to get wrong.

Two kinds of platform data must be treated differently:

- **Definitional / structural** — permissions, roles, role→permission mappings, menus.
  These *should be identical in every environment*. They are the application's
  **capability model**.
- **Operational / instance** — actual user accounts, who is assigned what, audit
  history. These *differ per environment* (test users locally, real users in prod).

The need: a safe, first-class way to move the **definitional** config between
environments (and to express it as code applied on deploy), without dragging operational
data or credentials across, and without each consumer re-inventing a seeder.

## Decision

### 1. Scope — what the bundle carries

- **Always (definitional):** permissions (`code`), roles (`tenant + code`),
  role→permission mappings (by code), menus (`tenant + code`, with parent and
  required-permission referenced **by code**).
- **Optional, guarded (operational):** users + their role/group assignments. **Off by
  default.** When enabled it **never** includes password hashes and **never** overwrites
  an existing target user (insert-new-only). This is the one sharp edge — see §5.
- **Never:** audit logs (per-environment history).
- **ABAC policies are out of scope** — they are *code* (`PolicyEvaluator` beans), not
  data. A JSON bundle cannot carry behaviour. The Policies admin screen lists and
  dry-run-tests them; "defining" a policy is a deploy of the app.
- **Tenant:** in single-tenant the bundle is scoped to the one tenant (`default`); the
  tenant row itself is environment configuration and is not synced by default. In
  multi-tenant, sync runs per selected tenant.

### 2. Mechanism — an export/import bundle, not a live API-to-API push

- **`GET /admin/api/v1/config/export`** → one JSON document (versioned schema) of the
  in-scope config, keyed by **natural keys** (codes), never DB UUIDs.
- **`POST /admin/api/v1/config/import?mode=merge&dryRun=true`** → upsert by natural key,
  transactional on the target, returning a **diff** (what would be created / updated /
  removed). `dryRun=true` returns the diff **without** applying it.
- **Convenience "push to target":** the *source backend* (not the browser) can POST its
  own export to a target's import endpoint given `{ targetBaseUrl, targetToken }` —
  server-to-server, so there is no CORS and the target's credentials never live in the
  browser. This is pure sugar over export + import.

**Why export/import is the primary model (and a live push is not):** the bundle is a
git-committable artifact. That single design also solves the *deploy / config-as-code*
problem — commit the exported JSON, review it in a PR, and run the same import on deploy
as the seeder. One feature, three uses: **env→env sync, version-controlled config, and
deploy seeding.** It is atomic on the target, has a built-in dry-run, and avoids the
many-round-trip partial-failure of a live push.

### 3. Identity matching — natural keys, never UUIDs

The kit's config entities already carry stable natural keys, so cross-environment upsert
is well-defined:

- permission `code` (globally unique), role `(tenant_id, code)`, menu `(tenant_id, code)`;
- menu→permission is already stored as `required_permission_code` (a code, not an id).

The bundle references everything by code (a role's permissions by permission code, a
menu's parent by parent code, …). UUIDs are assigned independently in each environment and
never cross the boundary.

### 4. Merge semantics + safety

- **`mode=merge` (default):** additive, idempotent upsert — create missing, update
  changed, **never delete**. Safe for promoting into an environment that may have its own
  extra config.
- **`mode=mirror` (opt-in):** make the target exactly match the bundle, **including
  deletes**. Dangerous (wipes target-only config); explicit opt-in and always preceded by
  a dry-run diff.
- **Dry-run is the default in the UI:** show "create role X, update menu Y, no deletes" →
  confirm → apply.
- Import requires the `admin.config.sync` permission (covered by `admin.*`), and **every
  import is written to the audit log**.

### 5. Gating — off by default, dev-only, fail-fast in production

- Property **`devslab.kit.config-sync.enabled`** (boolean, **default `false`**).
- The feature's beans and endpoints activate only when `enabled=true` **and** a non-prod
  profile is active (e.g. `local` / `dev`). Production — which doesn't run that profile —
  **cannot turn it on by construction**. There is deliberately **no "is-prod" heuristic**;
  we invert the question and require an explicit *dev* signal to enable.
- If `enabled=true` while a **prod** profile is active → **fail fast at startup** with a
  clear message. We do *not* silently disable it: a silent flip hides misconfiguration and
  wastes hours ("why isn't sync working?").
- `enabled=true` must live in environment-scoped config
  (`application-local.yaml` / a developer's env var), never the shared `application.yaml`.
- **Rationale:** the live sync/push surface is a **dev/staging convenience**. Production
  config arrives via the git-committed bundle applied on deploy (the seeding path), which
  is reviewed — not via an ad-hoc push from a developer's laptop.

### 6. Module placement / wire

- Endpoints in `devslab-kit-admin-api` (`config/ConfigSyncController`); the
  export/import/diff service in a `-core` module (new `devslab-kit-config-sync-core`, or
  folded into an existing one); gating in `devslab-kit-autoconfigure`. The admin console
  gets a **Config Sync** page: export (download), import (upload → dry-run diff → apply),
  and the optional push-to-URL.

## Consequences

**Positive**
- Reproducible environment→environment promotion of the capability model.
- The *same* bundle doubles as version-controlled config **and** the deploy seed — no
  bespoke per-consumer seeder.
- The natural-key design is already in place, so cross-env upsert is well-defined.
- Safe by default: off, dev-only, `merge` (no deletes), dry-run-first, audited.

**Negative / cost**
- A real feature: endpoints + bundle serialization + upsert + dry-run diff + admin-ui page
  + gating. The bundle schema must be **versioned** for forward/back compatibility.
- Importing into an environment where config was hand-edited needs the merge/dry-run
  discipline to avoid surprises.
- Optional **user sync** is the sharp edge — it must never carry password hashes and never
  overwrite an existing target user. Kept off by default for exactly this reason.

## Implementation plan (PR breakdown)

1. **Bundle schema + `GET /config/export`** (definitional scope) + a `ConfigBundle`
   record. Unit tests on the shape and code-keying.
2. **`POST /config/import`** with `merge` + `dryRun` diff (upsert by code, transactional).
   Real-Postgres Testcontainers test: export from one schema, import into a fresh one,
   assert the two are equal.
3. **Gating**: `config-sync.enabled` + the dev-profile condition + the prod fail-fast
   (a `FailureAnalyzer` for a friendly message).
4. **admin-ui "Config Sync" page** (export / import / dry-run diff / apply) + the optional
   server-to-server push.
5. **`mirror` mode + optional user sync** (guarded) — a separate PR, after `merge` is
   proven.
6. **Docs**: a guide ("promote platform config across environments"), document the
   deploy-seeding usage, and flip this ADR to **Accepted**.

## Alternatives considered

- **A live API-to-API push as the primary model** (the first instinct): rejected as the
  main mechanism — many round-trips, weak atomicity, target credentials in the browser if
  UI-driven, and no git-committable artifact. Kept only as optional server-to-server sugar
  on top of export/import.
- **`pg_dump`/restore of the `platform_*` tables**: rejected as the pipeline — it drags
  UUIDs and FKs, has no merge or dry-run, is all-or-nothing, and is not reviewable. Fine
  as a one-off manual escape hatch.
- **A hand-written seeder per consumer** (the status quo): works but is bespoke and not
  shareable. The export bundle generalizes it, and the import endpoint *is* the reusable
  seeder.
- **Sync everything, including users and audit logs**: rejected — pushing credentials/PII
  toward production is a security hazard, and audit is per-environment history.
  Definitional-only by default; users opt-in and guarded.
