# Tutorial: from zero to a running app

This is a complete, copy-paste walkthrough for someone who has **never used
devslab-kit**. By the end you'll have a Spring Boot app with login, an admin user,
roles & permissions, a permission-protected endpoint of your own, tenant-scoped
data, and an ABAC policy — all running locally.

No prior knowledge of the kit is assumed. Every command and file is shown in full.

!!! info "What you need first"
    - **JDK 21** (`java -version` should print 21).
    - **Docker** (to run PostgreSQL — `docker info` should succeed).
    - A terminal. An IDE (IntelliJ / VS Code) is nice but not required.

---

## Step 1 — Create a Spring Boot project

Generate a minimal Spring Boot 4 project (e.g. at [start.spring.io](https://start.spring.io)
choose Gradle + Java 21 + Spring Boot 4.x), or create the files below by hand in an
empty folder `myapp/`. By the end your project looks exactly like this — each file's full
path is shown above its contents in every step:

```text
myapp/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ compose.yaml
└─ src/
   └─ main/
      ├─ java/com/example/myapp/MyappApplication.java
      └─ resources/application.yml
```

**`myapp/settings.gradle.kts`**

```kotlin
rootProject.name = "myapp"
```

**`myapp/build.gradle.kts`**

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

dependencies {
    // The platform: authentication, RBAC + groups + ABAC, multi-tenancy,
    // dynamic menus, audit logging, and an admin REST API — all auto-configured.
    implementation("kr.devslab:devslab-kit-spring-boot-starter:0.4.2")

    // devslab-kit is unopinionated about which Spring starters you bring.
    // For this tutorial we want web + security + JPA + Flyway + PostgreSQL.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Lets `bootRun` start the Postgres container in compose.yaml automatically.
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}
```

Add the Gradle wrapper if you don't have one: `gradle wrapper` (or copy `gradlew`
from any Spring project).

You also need a main class — **`myapp/src/main/java/com/example/myapp/MyappApplication.java`**:

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyappApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyappApplication.class, args);
    }
}
```

---

## Step 2 — Start PostgreSQL with Docker

The kit stores everything in PostgreSQL. Create **`myapp/compose.yaml`**:

```yaml
services:
  postgres:
    image: 'postgres:16-alpine'
    environment:
      - 'POSTGRES_DB=myapp'
      - 'POSTGRES_USER=myapp'
      - 'POSTGRES_PASSWORD=myapp'
    ports:
      - '5432:5432'
```

Because you added `spring-boot-docker-compose`, **Spring Boot starts this container
for you** when you run the app — you don't need to `docker compose up` yourself.

---

## Step 3 — Configure the app

Create **`myapp/src/main/resources/application.yml`**:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myapp
    password: myapp
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate   # the kit owns its schema via Flyway; don't let Hibernate touch it

devslab:
  kit:
    tenant:
      mode: single             # one tenant for the whole app
      resolver: fixed
      default-tenant-id: default
    identity:
      jwt:
        secret: dev-only-change-me-32-bytes-minimum!   # ≥ 32 bytes; use a secret in prod
        ttl: PT8H              # access token lifetime (ISO-8601 duration)
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # provision the first admin on first boot
      admin-login-id: admin
      admin-password: admin    # dev only — see the warning below
      must-change-password: false
```

!!! warning "These are dev-only values"
    In production: set a real `identity.jwt.secret`, and for the bootstrap either
    set a strong `admin-password` (with `must-change-password: true`) or leave it
    blank to have a random one generated and logged once. See
    [Configuration](../reference/configuration.md#first-admin-bootstrap-devslabkitbootstrap).

---

## Step 4 — Run the app

Run it the way you normally develop:

=== "IntelliJ IDEA (typical)"

    Open the `myapp` folder as a Gradle project, let the import finish, then **Run**
    `MyappApplication` (the green ▶ by `main`).

    !!! tip "If Run fails right after adding/bumping the kit"
        A `ClassNotFoundException: kr.devslab.kit.*` means IntelliJ's project model is
        stale — **reload Gradle** (Gradle tool window → the ↻ *Reload* button) so it
        picks up the new jars, then Run again. (Gradle `bootRun` always works because it
        resolves fresh.)

=== "Terminal"

    ```bash
    ./gradlew bootRun
    ```

On first start the kit:

1. starts the Postgres container (via Docker Compose),
2. runs **Flyway** to create its `platform_*` tables (on a dedicated history table,
   so your own future migrations under `db/migration` won't collide),
3. **bootstraps** a tenant, a `PLATFORM_ADMIN` role with every `admin.*` permission,
   and an `admin` user,
4. serves the **admin REST API** at `/admin/api/v1/**` and **Swagger UI** at
   `/swagger-ui.html`.

The app is now listening on `http://localhost:8080`.

---

## Step 5 — Open the admin console

Day to day you manage the platform from the **web console**, not curl. Clone and run it
(in its own folder, alongside your app):

```bash
git clone https://github.com/devslab-kr/devslab-kit-admin-ui.git
cd devslab-kit-admin-ui
npm install
npm run dev
```

Open **http://localhost:5173** — the dev server proxies `/admin/api` to your app on
`:8080`. Sign in with the bootstrap admin: tenant `default`, login `admin`, password
`admin`. From here everything in the next steps is a few clicks — the
[Admin Console guide](../guides/admin-console.md) walks through every screen.

!!! note "Prefer the API / scripting it?"
    Everything also works over REST. Log in to get a JWT and reuse it as `$TOKEN`:
    ```bash
    TOKEN=$(curl -s localhost:8080/admin/api/v1/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","loginId":"admin","rawPassword":"admin"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
    echo "$TOKEN"
    ```

---

## Step 6 — Create a permission, a role, and a user

A **permission** is a string code (`resource.action`); a **role** bundles permissions; a
**user** holds roles. Let's give a new user the ability to read books — do it whichever way
suits you:

=== "In the admin console (clicks)"

    1. **Permissions → Create** — resource `book`, action `read`; the code previews as `book.read`. **Create**.
    2. **Roles → Create** — code `LIBRARIAN`, name `Librarian`. Then on its row click **Manage permissions** (key icon) and move `book.read` to *Assigned* → **Save**.
    3. **Users → Create** — login `alice`, a password. Then on her row click **Manage roles** (id-card icon) and add `LIBRARIAN` → **Save**.

    Every screen/button is detailed in the [Admin Console guide](../guides/admin-console.md).

=== "Via the API (curl)"

    Uses the `$TOKEN` from Step 5.

    ```bash
    # 1) create a permission  ->  note its "id" in the response
    curl -s -X POST localhost:8080/admin/api/v1/permissions \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d '{"code":"book.read","description":"Read books"}'

    # 2) create a role  ->  note its "id"
    curl -s -X POST localhost:8080/admin/api/v1/roles \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","code":"LIBRARIAN","name":"Librarian"}'

    # 3) grant the permission to the role  (use the ids from steps 1 & 2)
    curl -s -X POST "localhost:8080/admin/api/v1/roles/<ROLE_ID>/permissions/<PERMISSION_ID>" \
      -H "Authorization: Bearer $TOKEN"

    # 4) create a user
    curl -s -X POST localhost:8080/admin/api/v1/users \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d '{"tenantId":"default","loginId":"alice","rawPassword":"alice-password","email":"alice@example.com"}'

    # 5) assign the role to the user  (ids from steps 2 & 4)
    curl -s -X POST "localhost:8080/admin/api/v1/roles/<ROLE_ID>/users/<USER_ID>?tenantId=default" \
      -H "Authorization: Bearer $TOKEN"
    ```

Now `alice` can sign in (console or API) and holds `book.read`.

---

## Step 7 — Protect your own endpoint

The kit exposes a `PermissionChecker` bean. Inject it and gate your code:

```java
package com.example.myapp;

import kr.devslab.kit.access.Permission;
import kr.devslab.kit.access.PermissionChecker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BookController {

    private final PermissionChecker access;

    BookController(PermissionChecker access) {
        this.access = access;
    }

    @GetMapping("/api/books")
    String listBooks() {
        // Throws PermissionDeniedException (-> 403) if the caller lacks it.
        access.check(Permission.of("book.read"));
        return "here are the books";
    }
}
```

Call it with alice's token → `200`; with a user who lacks `book.read` → `403`.
Other methods: `hasPermission(...)`, `hasAnyPermission(...)`, `hasAllPermissions(...)`.

---

## Step 8 — Know who's calling, and scope data to the tenant

Two more beans you'll use constantly:

```java
import kr.devslab.kit.identity.CurrentUserProvider;
import kr.devslab.kit.tenant.TenantContextHolder;

// who is the authenticated user?
String loginId = currentUserProvider.current()
        .map(u -> u.loginId())
        .orElseThrow();

// which tenant is this request for? (always present — single-tenant resolves "default")
String tenantId = tenantContextHolder.current()
        .map(ctx -> ctx.tenantId().value())
        .orElseThrow();
```

Store `tenantId` on your own entities and filter every query by it — that's all
multi-tenancy needs. Your code is identical whether you run in `single` or `multi`
mode (see the [Multi-tenancy guide](../guides/tenancy.md)).

---

## Step 9 — Add an attribute-based (ABAC) rule

RBAC answers "does the user hold `book.read`?". ABAC adds "…**for this specific
book, right now?**". You write a **`Policy`** bean; the kit collects it and
dispatches by name.

```java
package com.example.myapp;

import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import org.springframework.stereotype.Component;

@Component
class BookOwnerPolicy implements Policy {

    @Override public String name() { return "book-owner"; }

    @Override
    public PolicyDecision evaluate(PolicyContext ctx) {
        // e.g. only the owner may touch the book
        Object owner = ctx.resourceAttributes().get("ownerLoginId");
        boolean isOwner = ctx.userId().isPresent() && /* compare to owner */ owner != null;
        return isOwner ? PolicyDecision.PERMIT : PolicyDecision.DENY;
    }
}
```

Gate with the ABAC-aware overload of `check`, building the context with the builder:

```java
PolicyContext ctx = PolicyContext.builder()
        .user(currentUserId)
        .tenant(currentTenantId)
        .resource("book", bookId)
        .resourceAttributes(java.util.Map.of("ownerLoginId", book.getOwnerLoginId()))
        .build();

access.check(Permission.of("book.read"), "book-owner", ctx);
```

**Verify a policy without writing any code** from the admin console's **Policies**
page (or `POST /admin/api/v1/policies/test`): pick the policy, fill in a subject /
resource / environment, and it returns `PERMIT` / `DENY` / `NOT_APPLICABLE` with the
reason and matched rules. See the [Access guide](../guides/access.md#abac-policies).

---

## You're done 🎉

You now have a running platform app. Where to go next:

- **[Multi-tenancy](../guides/tenancy.md)** — resolvers, `single` vs `multi`.
- **[Access (RBAC + ABAC)](../guides/access.md)** — groups, the full policy model.
- **[Dynamic menus](../guides/menus.md)** · **[Audit logging](../guides/audit.md)** · **[Caching](../guides/cache.md)**
- **[Admin REST API](../reference/admin-api.md)** · **[Configuration reference](../reference/configuration.md)**
- A complete, runnable example app lives in
  [`devslab-kit-sample-app`](https://github.com/devslab-kr/devslab-kit/tree/main/devslab-kit-sample-app).
