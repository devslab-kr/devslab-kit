# 빠른 시작

인증, 멀티테넌시, 관리자 API, 그리고 시드된 관리자 사용자를 갖춘 동작하는 앱을 부팅합니다.

## 1. 스타터 추가

[설치](installation.md)를 참고하세요.

## 2. 설정

앱을 PostgreSQL(선택적으로 Redis)에 연결한 뒤 플랫폼 설정을 지정합니다:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: app
    password: app
  data:
    redis:
      host: localhost          # cache.type = redis 일 때만 필요

devslab:
  kit:
    tenant:
      mode: single             # single | multi
      resolver: fixed          # fixed | header | jwt | subdomain
      default-tenant-id: default
    identity:
      jwt:
        secret: ${DEVSLAB_JWT_SECRET}   # HS256용 32바이트 이상
        ttl: PT8H
    cache:
      type: in-memory          # in-memory | redis | none
    bootstrap:
      enabled: true            # 첫 부팅 시 최초 관리자 생성
```

모든 키는 [설정 레퍼런스](../reference/configuration.md)를 참고하세요.

## 3. 부팅

첫 시작 시 kit은:

1. Flyway로 `platform_*` 테이블을 생성하고,
2. 테넌트, `PLATFORM_ADMIN` 역할, `admin.*` 권한, 관리자 사용자를 생성하며
   ([최초 관리자 부트스트랩](../guides/bootstrap.md)),
3. `/admin/api/v1/**`에서 관리자 REST API를 제공합니다.

```bash
./gradlew bootRun
```

## 4. 로그인

관리자 API를 직접 호출하거나,
[관리자 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)을 연결하세요:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"<부트스트랩 비밀번호>"}'
```

`bootstrap.admin-password`를 비우면 kit이 강력한 랜덤 비밀번호를 만들어 시작 시 한 번
로깅합니다. 알려진 값을 쓰려면 명시적으로 설정하세요.

## 다음

- [멀티테넌시](../guides/tenancy.md) · [접근 제어 (RBAC + ABAC)](../guides/access.md) · [캐시](../guides/cache.md)
- [관리자 REST API](../reference/admin-api.md)
- 완전히 동작하는 설정은
  [`devslab-kit-sample-app`](https://github.com/devslab-kr/devslab-kit/tree/main/devslab-kit-sample-app)에
  있습니다.
