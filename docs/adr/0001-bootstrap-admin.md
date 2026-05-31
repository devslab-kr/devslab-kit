# ADR 0001 — First-admin bootstrap across environments

- **Status:** Accepted
- **Date:** 2026-05-30 (accepted 2026-05-31)
- **Languages:** [English](0001-bootstrap-admin.md) · [한국어](0001-bootstrap-admin.ko.md)
- **Implemented by:** the first-admin bootstrap feature — `must_change_password`
  (V11) + self-service `POST /admin/api/v1/auth/change-password` +
  `BootstrapAutoConfiguration` / `DevslabKitBootstrapRunner`, with the
  `sample-app` switched off its old seed runner. The admin-ui forced-change
  guard + screen lands as a follow-up in `devslab-kit-admin-ui`.

## Context

`devslab-kit` ships authentication, RBAC + groups + ABAC, multi-tenancy, menus,
audit, and an admin REST API, consumed by the `devslab-kit-admin-ui` dashboard.

There is a chicken-and-egg problem: a fresh database has **zero user accounts**,
so nobody can log in to the dashboard, so nobody can create menus, grant
permissions, or add the *real* administrators. Every system needs a way to
mint a **first administrator**.

The hard part is doing this **without leaving a permanent backdoor**, while the
*same artifact* moves through three environments with different needs:

| Environment | What the operator wants |
| --- | --- |
| **local-dev** | `admin / admin`, log in instantly, ideally skip the password-change step for speed |
| **staging** | Bootstrap, but with an injected password and forced change (a production rehearsal) |
| **production (운영)** | No fixed default password, ever. Injected password + forced change, or no auto-bootstrap at all |

### Why "just use a Spring profile" is not enough

Gating bootstrap on `@Profile("dev")` is tempting but flawed:

- **Profiles belong to the consumer.** A library cannot reliably key off a
  profile name — consumers call it `local` / `dev` / `development` inconsistently.
- **Profiles are on/off only.** They cannot express "was a password injected?"
  or "is forced-change on?".
- **A profile slip is a security incident.** Turning on `dev` in production
  would silently create an `admin/admin` backdoor.

The industry-standard approach is **explicit properties plus safety guards**,
where a profile (if used at all) is just the consumer's *mechanism* for turning
those properties on — never the kit's trigger.

## Decision

### 1. Bootstrap is property-driven, OFF by default

A new `devslab.kit.bootstrap.*` configuration block, disabled unless the
consumer explicitly opts in:

```yaml
devslab.kit.bootstrap:
  enabled: false                 # default — a no-config prod deploy never bootstraps
  tenant-id: default             # tenant the first admin belongs to
  admin-login-id: admin
  admin-password:                # blank → generate a random one and log it once
  admin-email: admin@example.com
  must-change-password: true     # default ON — the admin must rotate on first login
  role-code: PLATFORM_ADMIN
```

Because the default is `enabled: false`, deploying the artifact with no
bootstrap config — the production default — provisions nothing. The backdoor
cannot exist by accident.

### 2. No fixed default password — blank means "random, logged once"

When `enabled: true` but `admin-password` is blank, the runner generates a
strong random password and writes it to the boot log **exactly once**:

```
============================================================
 devslab-kit bootstrap: created first admin
   tenant : default
   login  : admin
   password (shown ONCE — copy it now): a8Kx29...
   This account must change its password on first login.
============================================================
```

This is the GitLab / Jenkins / Sonatype pattern. A fixed `admin/admin` can
**only** appear if the operator wrote `admin-password: admin` themselves — which
is exactly what a local-dev profile does, and exactly what a production config
must not.

### 3. Forced password change on first login

A new `must_change_password` flag on the user account (default `true` for
bootstrapped admins). While the flag is set:

- Login still authenticates and issues a token, but the token / login response
  carries `mustChangePassword: true`.
- The dashboard, seeing that flag, routes the user to a **change-password
  screen** and blocks every other route until the password is rotated.
- A new self-service endpoint `POST /admin/api/v1/auth/change-password`
  (old + new password) clears the flag. This is distinct from the existing
  admin-resets-someone-else's-password endpoint.

So the full operator story works: log in with the bootstrap password → forced
to set a new one → now a normal admin → create menus, grant permissions, add
real administrators → optionally disable or delete the bootstrap admin.

### 4. Per-environment usage (consumer side)

The kit stays environment-agnostic; the consumer expresses intent in
profile-specific config files:

```yaml
# application.yml  (shared) — nothing here = production-safe by default

# application-local.yml
devslab.kit.bootstrap:
  enabled: true
  admin-password: admin
  must-change-password: false        # local: log straight in

# application-staging.yml
devslab.kit.bootstrap:
  enabled: true
  admin-password: ${BOOTSTRAP_ADMIN_PW}   # injected secret
  must-change-password: true

# production
#   Option A: same as staging with an injected secret + forced change.
#   Option B: leave bootstrap.enabled=false and provision the first admin
#             out-of-band (one-off job / SQL / a CLI), so the running app
#             never carries a bootstrap path at all.
```

### 5. Idempotency and a production safety pin

- **Idempotent:** the runner checks for an existing user by `(tenant, loginId)`
  and skips creation if present. A staging database promoted to production, or
  any restart, re-runs the runner as a no-op.
- **Safety pin (optional, default on):**
  `devslab.kit.bootstrap.fail-on-default-password-in-prod: true` — if the active
  profiles include `prod`/`production` **and** the resolved password equals a
  well-known weak value (`admin`, `password`, `changeme`, …), the app fails to
  start with a clear message. This is a backstop, not the primary control (the
  primary control is "no fixed default exists").

### 6. Frontend dashboard implications (forward-looking)

- **Now:** the dashboard must handle `mustChangePassword` — a guard that
  diverts to the change-password screen and a small view calling the new
  endpoint. (Tracked as a follow-up UI PR.)
- **Later — a guided first-run / setup wizard:** instead of `admin/admin`, the
  very first dashboard visit on an un-provisioned instance could present a
  one-time "create the first administrator" form (à la Jenkins setup, GitLab
  root password screen). That replaces bootstrap-by-config for interactive
  installs while the property path remains for headless / automated deploys.
  Out of scope for this ADR; recorded so the bootstrap contract leaves room
  for it (e.g. a `GET /admin/api/v1/bootstrap/status` returning
  `{ initialized: boolean }` the wizard can branch on).

## Consequences

**Positive**
- One artifact, three environments, no rebuild — behaviour is config, not code.
- Production-safe by default (OFF), and even when on, no fixed password can leak.
- The full "log in → rotate → hand off → remove bootstrap admin" lifecycle is
  expressible.
- The `sample-app`'s current `SampleSeedRunner` collapses into this one
  mechanism (`bootstrap.enabled=true, admin-password=admin,
  must-change-password=false` in its dev config), removing duplicated logic.

**Negative / cost**
- Several moving parts: a schema migration (`must_change_password`), an
  identity field, a self-service change-password endpoint + login-response
  field, the autoconfig runner, and a UI guard + screen. Best split across a
  few PRs (see Implementation plan).
- Forced-change adds one login round-trip in dev unless explicitly turned off.
- The random-password-in-logs pattern assumes operators can read the boot log
  on first start; documented prominently.

## Implementation plan (PR breakdown)

1. **identity: `must_change_password`** — migration `V11`, entity field,
   `CurrentUser` field, surfaced in the login response + JWT claim.
2. **identity: self-service change-password** — `LocalLoginService` /
   account service method that verifies the old password, sets the new one,
   clears the flag; `POST /admin/api/v1/auth/change-password`.
3. **autoconfigure: `BootstrapAutoConfiguration` + `DevslabKitBootstrapRunner`**
   — the property block, random-password generation + one-time log, idempotent
   provisioning, the prod safety pin. `sample-app` switches to it and deletes
   `SampleSeedRunner`.
4. **admin-ui: forced-change guard + screen** — router guard on
   `mustChangePassword`, a change-password view, wire to the new endpoint.
5. **docs:** update both READMEs' "first run" sections to reference this ADR;
   add the per-environment config snippets.

## Alternatives considered

- **Profile-gated bootstrap (`@Profile("dev")`):** rejected — see *Context*.
- **`first-run` auto-create with a fixed default, no opt-in:** rejected — a
  fixed default in a library is a latent production backdoor.
- **`fail-fast` when no password injected (even in dev):** viable and stricter,
  but worse local-dev ergonomics; the random-password-logged path gives the
  same prod safety with a friendlier default. Kept as the consumer's choice
  (they can simply always inject a password).
- **No bootstrap at all (consumer's problem):** safest, but fails the "drop in
  the starter and start" goal that motivated the question.
