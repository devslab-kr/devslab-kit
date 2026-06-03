# 멀티테넌시

**테넌트**는 격리된 작업공간 — 한 고객/조직과 그 모든 데이터입니다. `devslab-kit`에는 **항상
테넌트가 컨텍스트에 존재**합니다. 싱글 테넌트 앱이라도 추상화를 건너뛰지 않고 `default` 테넌트를
resolve하므로, **코드는 고객이 한 곳이든 수천이든 동일**합니다. "테넌트 없음" 특수 처리를 할 일이
없습니다.

사전 지식 없다고 가정합니다. 처음이면 [튜토리얼](../getting-started/tutorial.md)부터 — 8단계가
실행 중인 앱에서 테넌시를 보여줍니다.

## 모드 선택

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    tenant:
      mode: single            # single | multi
      resolver: fixed         # fixed | header | jwt | subdomain
      default-tenant-id: default
```

| `mode` | 언제 | 동작 |
| --- | --- | --- |
| `single` | 고객 한 곳 / 내부 도구 | 모든 요청이 `default-tenant-id` 로 resolve |
| `multi`  | 다수 고객 SaaS | **resolver**가 요청마다 테넌트 결정 |

`single` + `fixed` 로 시작하세요. 두 번째 테넌트를 실제로 온보딩할 때 `multi` 로 전환 — 코드 변경
없이 설정만 바꾸면 됩니다.

## 리졸버 (멀티 테넌트)

`multi` 모드에서 **리졸버**가 이 요청이 누구 것인지 결정합니다:

| `resolver` | 테넌트를 무엇에서 | 예 |
| --- | --- | --- |
| `fixed` | 항상 `default-tenant-id` | (싱글 테넌트 기본) |
| `header` | 요청 헤더(기본 `X-Tenant-Id`) | `X-Tenant-Id: acme` |
| `jwt` | 인증된 JWT의 클레임 | 로그인 사용자의 테넌트 |
| `subdomain` | 요청 호스트의 서브도메인 | `acme.app.com` → `acme` |

```yaml
devslab:
  kit:
    tenant:
      mode: multi
      resolver: header
      header: X-Tenant-Id     # header 리졸버만 사용
```

```bash
# header 리졸버면 모든 요청이 테넌트를 실어 보냄:
curl localhost:8080/api/invoices -H 'X-Tenant-Id: acme'
```

## 코드에서 사용

### 현재 테넌트 읽기

`TenantContextHolder`는 현재 요청에 바인딩된 테넌트를 담습니다(당신 코드 실행 전에 kit이 설정):

```java
// src/main/java/com/example/myapp/InvoiceService.java
import kr.devslab.kit.tenant.TenantContextHolder;

@Service
class InvoiceService {

    private final TenantContextHolder tenantContext;
    private final InvoiceRepository invoices;

    InvoiceService(TenantContextHolder tenantContext, InvoiceRepository invoices) {
        this.tenantContext = tenantContext;
        this.invoices = invoices;
    }

    private String currentTenant() {
        return tenantContext.current()
                .orElseThrow(() -> new IllegalStateException("no tenant in context"))
                .tenantId().value();
    }

    List<Invoice> list() {
        return invoices.findByTenantId(currentTenant());   // 테넌트 간 누수 금지
    }

    Invoice create(String amount) {
        return invoices.save(new Invoice(UUID.randomUUID(), currentTenant(), amount));
    }
}
```

(웹 요청 *밖*에서 테넌트를 resolve해야 하면 — 예: 스케줄 잡 — `TenantResolver`를 주입:
`tenantResolver.resolve().tenantId().value()`.)

### 데이터를 테넌트 단위로 격리

규칙은 단순합니다: **테넌트 소유 엔터티마다 `tenant_id`를 두고 모든 쿼리를 그걸로 필터링.**

```java
// src/main/java/com/example/myapp/Invoice.java
@Entity
class Invoice {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    private String amount;
    // 생성자 + getter …
}
```

```java
// src/main/java/com/example/myapp/InvoiceRepository.java
interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByTenantId(String tenantId);
    Optional<Invoice> findByIdAndTenantId(UUID id, String tenantId);   // 단건 조회도
}
```

이게 패턴의 전부 — `single`/`multi` 동일합니다.

## 커스텀 리졸버

내장으로 안 되는 전략(DB 조회, header-or-path, API 키 → 테넌트 매핑)이 필요하면, 자신의
`TenantResolver` 빈을 선언하면 kit 기본이 물러납니다(모든 kit 빈이 `@ConditionalOnMissingBean`):

```java
// src/main/java/com/example/myapp/ApiKeyTenantResolver.java
import kr.devslab.kit.tenant.TenantResolver;
import kr.devslab.kit.tenant.TenantContext;
import kr.devslab.kit.core.id.TenantId;

@Component
class ApiKeyTenantResolver implements TenantResolver {

    private final HttpServletRequest request;   // request-scoped
    private final TenantDirectory directory;     // 당신의 조회기

    ApiKeyTenantResolver(HttpServletRequest request, TenantDirectory directory) {
        this.request = request;
        this.directory = directory;
    }

    @Override
    public TenantContext resolve() {
        String apiKey = request.getHeader("X-Api-Key");
        String tenantId = directory.tenantForApiKey(apiKey);   // 예: DB 조회
        return TenantContext.of(TenantId.of(tenantId));
    }
}
```

## 테넌트 관리

테넌트 생성 / 정지 / 보관은 admin 콘솔 **Tenants** 화면(또는 `tenants` REST 엔드포인트)에서 —
[Admin 콘솔 가이드](admin-console.md#tenants) 참고.

모든 키는 [설정 레퍼런스](../reference/configuration.md#tenant) 참고.
