# 튜토리얼: 0에서 실행까지

**devslab-kit을 한 번도 안 써본 사람**을 위한, 복붙으로 따라 하는 완전 가이드입니다. 끝까지
하면 로그인, 관리자 계정, 역할·권한, 직접 만든 권한 보호 엔드포인트, 테넌트 단위 데이터, ABAC
정책까지 **로컬에서 동작**합니다.

사전 지식 없다고 가정합니다. 모든 명령과 파일을 전부 보여줍니다.

!!! info "먼저 필요한 것"
    - **JDK 21** (`java -version` 이 21을 출력)
    - **Docker** (PostgreSQL 실행용 — `docker info` 가 성공해야 함)
    - 터미널. IDE(IntelliJ / VS Code)는 있으면 좋지만 필수는 아님.

---

## 1단계 — Spring Boot 프로젝트 만들기

최소 Spring Boot 4 프로젝트를 생성합니다([start.spring.io](https://start.spring.io)에서 Gradle +
Java 21 + Spring Boot 4.x). 또는 빈 폴더 `myapp/` 에 아래 두 파일만 만들어도 됩니다.

**`settings.gradle.kts`**

```kotlin
rootProject.name = "myapp"
```

**`build.gradle.kts`**

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
    // 플랫폼: 인증, RBAC + 그룹 + ABAC, 멀티테넌시, 동적 메뉴, 감사 로깅,
    // 관리자 REST API — 전부 자동 구성.
    implementation("kr.devslab:devslab-kit-spring-boot-starter:0.4.2")

    // devslab-kit은 어떤 Spring 스타터를 쓸지 강요하지 않습니다.
    // 이 튜토리얼에선 web + security + JPA + Flyway + PostgreSQL.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // `bootRun` 시 compose.yaml 의 Postgres 컨테이너를 자동으로 띄워줍니다.
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}
```

Gradle 래퍼가 없으면 추가하세요: `gradle wrapper` (또는 다른 Spring 프로젝트의 `gradlew` 복사).

메인 클래스도 필요합니다 — **`src/main/java/com/example/myapp/MyappApplication.java`**:

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

## 2단계 — Docker로 PostgreSQL 띄우기

kit은 모든 것을 PostgreSQL에 저장합니다. 프로젝트 루트에 **`compose.yaml`** 생성:

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

`spring-boot-docker-compose` 를 넣었으니 **앱 실행 시 Spring Boot가 이 컨테이너를 자동으로
시작**합니다 — 직접 `docker compose up` 안 해도 됩니다.

---

## 3단계 — 앱 설정

**`src/main/resources/application.yml`** 생성:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: myapp
    password: myapp
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate   # 스키마는 kit이 Flyway로 관리 — Hibernate가 건드리지 않게

devslab:
  kit:
    tenant:
      mode: single             # 앱 전체가 단일 테넌트
      resolver: fixed
      default-tenant-id: default
    identity:
      jwt:
        secret: dev-only-change-me-32-bytes-minimum!   # 32바이트 이상; 운영에선 시크릿 사용
        ttl: PT8H              # 액세스 토큰 수명 (ISO-8601 duration)
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # 첫 부팅 시 최초 관리자 생성
      admin-login-id: admin
      admin-password: admin    # 개발 전용 — 아래 경고 참고
      must-change-password: false
```

!!! warning "이 값들은 개발 전용입니다"
    운영에선: 실제 `identity.jwt.secret` 설정, bootstrap은 강한 `admin-password`(+
    `must-change-password: true`)를 쓰거나 비워서 랜덤 생성·1회 로깅되게 하세요.
    [설정 레퍼런스](../reference/configuration.md) 참고.

---

## 4단계 — 실행

```bash
./gradlew bootRun
```

첫 실행 시 kit은:

1. (Docker Compose로) Postgres 컨테이너를 시작하고,
2. **Flyway**로 `platform_*` 테이블 생성(전용 history 테이블을 써서, 나중에 당신이 `db/migration`
   에 추가할 마이그레이션과 충돌하지 않음),
3. 테넌트, 모든 `admin.*` 권한을 가진 `PLATFORM_ADMIN` 역할, `admin` 사용자를 **부트스트랩**,
4. `/admin/api/v1/**` 에 **관리자 REST API**, `/swagger-ui.html` 에 **Swagger UI** 제공.

이걸 띄워둔 채로 두 번째 터미널을 열어 다음 단계를 진행하세요.

---

## 5단계 — 로그인

모든 관리자 호출엔 토큰이 필요합니다. 부트스트랩 관리자로 로그인:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","rawPassword":"admin"}'
```

응답에 JWT가 들어있습니다. 다음 명령에서 재사용하도록 셸 변수에 담으세요:

```bash
TOKEN=$(curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","rawPassword":"admin"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo "$TOKEN"
```

!!! tip "UI가 편하시면"
    [admin 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)을 `http://localhost:8080`
    에 연결하면 6단계를 curl 대신 클릭으로 할 수 있습니다.

---

## 6단계 — 권한·역할·사용자 만들기

**권한**은 문자열 코드(`resource.action`), **역할**은 권한 묶음, **사용자**는 역할을 가집니다.
새 사용자에게 "책 읽기" 권한을 줘봅시다.

```bash
# 1) 권한 생성  ->  응답의 "id" 기록
curl -s -X POST localhost:8080/admin/api/v1/permissions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"code":"book.read","description":"책 읽기"}'

# 2) 역할 생성  ->  "id" 기록
curl -s -X POST localhost:8080/admin/api/v1/roles \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","code":"LIBRARIAN","name":"사서"}'

# 3) 역할에 권한 부여  (1·2의 id 사용)
curl -s -X POST "localhost:8080/admin/api/v1/roles/<ROLE_ID>/permissions/<PERMISSION_ID>" \
  -H "Authorization: Bearer $TOKEN"

# 4) 사용자 생성
curl -s -X POST localhost:8080/admin/api/v1/users \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"alice","rawPassword":"alice-password","email":"alice@example.com"}'

# 5) 사용자에게 역할 할당  (2·4의 id 사용)
curl -s -X POST "localhost:8080/admin/api/v1/roles/<ROLE_ID>/users/<USER_ID>?tenantId=default" \
  -H "Authorization: Bearer $TOKEN"
```

이제 `alice`로 로그인(5단계, alice 자격증명)하면 `book.read` 권한을 가집니다.

---

## 7단계 — 내 엔드포인트 보호하기

kit은 `PermissionChecker` 빈을 제공합니다. 주입해서 코드를 게이트하세요:

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
        // 권한이 없으면 PermissionDeniedException(-> 403) 발생.
        access.check(Permission.of("book.read"));
        return "here are the books";
    }
}
```

alice 토큰으로 호출 → `200`, `book.read` 없는 사용자 → `403`.
다른 메서드: `hasPermission(...)`, `hasAnyPermission(...)`, `hasAllPermissions(...)`.

---

## 8단계 — 호출자 파악 + 테넌트 단위로 데이터 격리

자주 쓰게 될 두 빈:

```java
import kr.devslab.kit.identity.CurrentUserProvider;
import kr.devslab.kit.tenant.TenantContextHolder;

// 인증된 사용자는 누구인가?
String loginId = currentUserProvider.current()
        .map(u -> u.loginId())
        .orElseThrow();

// 이 요청의 테넌트는? (항상 존재 — 단일 테넌트는 "default" 반환)
String tenantId = tenantContextHolder.current()
        .map(ctx -> ctx.tenantId().value())
        .orElseThrow();
```

당신의 엔터티에 `tenantId`를 저장하고 모든 쿼리를 그걸로 필터링하면 멀티테넌시 끝입니다.
`single`이든 `multi` 모드든 코드는 동일합니다([멀티테넌시 가이드](../guides/tenancy.md)).

---

## 9단계 — 속성 기반(ABAC) 규칙 추가

RBAC는 "사용자가 `book.read`를 가졌나?"에 답하고, ABAC는 "…**이 특정 책에 대해, 지금?**"까지
답합니다. **`Policy`** 빈을 작성하면 kit이 수집해 이름으로 디스패치합니다.

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
        // 예: 소유자만 그 책을 다룰 수 있음
        Object owner = ctx.resourceAttributes().get("ownerLoginId");
        boolean isOwner = ctx.userId().isPresent() && /* owner와 비교 */ owner != null;
        return isOwner ? PolicyDecision.PERMIT : PolicyDecision.DENY;
    }
}
```

ABAC 오버로드로 게이트하고, 컨텍스트는 빌더로 구성:

```java
PolicyContext ctx = PolicyContext.builder()
        .user(currentUserId)
        .tenant(currentTenantId)
        .resource("book", bookId)
        .resourceAttributes(java.util.Map.of("ownerLoginId", book.getOwnerLoginId()))
        .build();

access.check(Permission.of("book.read"), "book-owner", ctx);
```

**코드 없이 정책을 검증**하려면 admin 콘솔의 **Policies** 페이지(또는
`POST /admin/api/v1/policies/test`)에서 정책을 고르고 주체/자원/환경을 채우면
`PERMIT` / `DENY` / `NOT_APPLICABLE` 과 이유·매칭 규칙을 돌려줍니다.
[Access 가이드](../guides/access.md) 참고.

---

## 끝났습니다 🎉

이제 동작하는 플랫폼 앱이 생겼습니다. 다음으로:

- **[멀티테넌시](../guides/tenancy.md)** — 리졸버, `single` vs `multi`
- **[Access (RBAC + ABAC)](../guides/access.md)** — 그룹, 전체 정책 모델
- **[동적 메뉴](../guides/menus.md)** · **[감사 로깅](../guides/audit.md)** · **[캐시](../guides/cache.md)**
- **[관리자 REST API](../reference/admin-api.md)** · **[설정 레퍼런스](../reference/configuration.md)**
- 완전히 실행 가능한 예제 앱:
  [`devslab-kit-sample-app`](https://github.com/devslab-kr/devslab-kit/tree/main/devslab-kit-sample-app).
