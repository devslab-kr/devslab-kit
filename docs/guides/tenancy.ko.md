# 멀티테넌시

`devslab-kit`은 항상 **테넌트 컨텍스트** 안에서 동작합니다 — 싱글 테넌트 배포라도
추상화를 건너뛰지 않고 default 테넌트를 resolve하므로, 코드에서 "테넌트 없음"을 특수
처리할 일이 없습니다.

## 모드

| `tenant.mode` | 동작 |
| --- | --- |
| `single` | 테넌트 하나(`tenant.default-tenant-id`). 리졸버가 항상 그것을 반환. |
| `multi` | 선택한 리졸버가 요청마다 테넌트를 resolve. |

## 리졸버

`tenant.resolver`로 하나 선택:

| 리졸버 | 테넌트 결정 기준 |
| --- | --- |
| `fixed` | 항상 `tenant.default-tenant-id` (싱글 테넌트 기본값). |
| `header` | 요청 헤더(예: `X-Tenant-Id`). |
| `jwt` | 인증된 JWT의 클레임. |
| `subdomain` | 요청 호스트의 서브도메인(`acme.app.com` → `acme`). |

```yaml
devslab:
  kit:
    tenant:
      mode: multi
      resolver: header
```

## 사용

활성 테넌트를 resolve하려면 `TenantResolver`를(현재 요청에 바인딩된 것을 읽으려면
`TenantContextHolder`를) 주입하세요:

```java
import kr.devslab.kit.tenant.TenantResolver;

@Service
class ReportService {
    private final TenantResolver tenants;

    ReportService(TenantResolver tenants) { this.tenants = tenants; }

    void run() {
        String tenantId = tenants.resolve().tenantId().value();
        // … tenantId로 쿼리 범위 지정 …
    }
}
```

## Override

커스텀 결정 전략(DB 조회, 헤더+경로 조합 등)이 필요하면 직접 `TenantResolver` 빈을
선언하세요. kit의 기본 구현이 물러납니다:

```java
@Bean
TenantResolver tenantResolver() {
    return () -> /* 당신의 TenantContext */;
}
```

모든 키는 [설정 레퍼런스](../reference/configuration.md#tenant)를 참고하세요.
