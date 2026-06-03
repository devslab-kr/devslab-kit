# 감사 로깅

**감사 추적(audit trail)**은 누가·무엇을·언제·성공했는지를 영구히 남기는 기록입니다 — "사용자
`admin`이 14:03에 테넌트 `acme`를 정지함, 성공". kit은 이 추적을 요청의 임계 경로 밖에서
**비동기**로 기록하고, 각 이벤트를 메타데이터까지 JSONB로 PostgreSQL에 저장합니다.

처음이면 [튜토리얼](../getting-started/tutorial.md)부터 — 이 가이드는 실행 중인 앱이 있다고
가정합니다.

## 흐름

```
내 코드                       kit                              PostgreSQL
───────                       ───                              ──────────
audit.publish(event)  ──►  publish()가 비동기 executor에  ──►  감사 테이블 한 행,
                           넘김(요청 스레드 밖)                메타데이터는 JSONB
```

쓰기가 비동기이므로, 느리거나 실패하는 감사 쓰기가 그것을 유발한 요청을 느리게 하거나 실패
시키지 않습니다.

## 이벤트 기록

`AuditEventPublisher`를 주입하고 `AuditEvent`를 `publish`합니다. 이벤트는
`AuditEvent.builder()`로 만듭니다:

```java
// src/main/java/com/example/myapp/TenantAdminService.java
import java.time.Instant;
import java.util.Map;
import kr.devslab.kit.audit.AuditAction;
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.audit.AuditOutcome;
import kr.devslab.kit.audit.AuditTarget;

@Service
class TenantAdminService {

    private final AuditEventPublisher audit;

    TenantAdminService(AuditEventPublisher audit) { this.audit = audit; }

    void suspend(String tenantId, String reason) {
        // … 변경 수행 …

        audit.publish(AuditEvent.builder()
                .action(AuditAction.of("tenant.suspend"))      // 필수
                .target(new AuditTarget("tenant", tenantId))
                .outcome(AuditOutcome.SUCCESS)                 // SUCCESS | FAILURE
                .occurredAt(Instant.now())                     // 필수
                .metadata(Map.of("reason", reason))            // 자유 형식 → JSONB
                .build());
    }
}
```

### 필드

| 필드 | 필수 | 의미 |
| --- | --- | --- |
| `action` | **예** | 무슨 일인지, 고정 코드로: `AuditAction.of("tenant.suspend")`. |
| `occurredAt` | **예** | 언제 — `Instant.now()`. |
| `target` | 아니오 | 대상: `new AuditTarget(type, id)`, 예 `("tenant", "acme")`. |
| `outcome` | 아니오 | `AuditOutcome.SUCCESS` 또는 `AuditOutcome.FAILURE`. |
| `actor` | 아니오 | 행위자: `new AuditActor(userId, tenantId, displayName)`. |
| `metadata` | 아니오 | 추가 컨텍스트 `Map<String,Object>` — JSONB로 저장. |
| `ip` / `userAgent` | 아니오 | 요청 출처(있을 때). |

!!! warning "`occurredAt`은 필수, `actor`는 자동으로 안 채워짐"
    `occurredAt`이 없으면 `build()`가 예외를 던집니다 — 항상 설정하세요. `actor`는 선택이지만,
    kit이 보안 컨텍스트에서 **대신 채워주지 않습니다**: 행위자를 남기려면 `actor`를 직접
    설정하세요(예: `CurrentUser`에서).

## 추적 조회

=== "관리자 콘솔"

    [관리자 콘솔](admin-console.md) → **Audit Logs**: 필터(행위자 / 액션 / 대상 유형 / 결과 /
    기간)와 JSON 페이로드 상세 드로어를 갖춘, 검색 가능한 lazy 페이지 테이블.
    [관리자 콘솔 가이드 → Audit Logs](admin-console.md#audit-logs) 참고.

=== "REST API"

    ```bash
    # 액션 + 결과 + 기간으로 필터:
    curl -G localhost:8080/admin/api/v1/audit-logs \
      -H 'Authorization: Bearer <token>' \
      --data-urlencode 'tenantId=default' \
      --data-urlencode 'action=tenant.suspend' \
      --data-urlencode 'outcome=FAILURE' \
      --data-urlencode 'from=2026-06-01T00:00:00Z'
    ```

    페이지 목록을 반환합니다. 전체 `audit-logs` 쿼리 파라미터는
    [관리자 REST API](../reference/admin-api.md) 참고.

## 튜닝

| 키 | 기본값 | |
| --- | --- | --- |
| `audit.enabled` | `true` | 필요 없으면 전체 서브시스템 끄기. |
| `audit.async-queue-capacity` | `1024` | 비동기 writer로 들어가는 bounded 큐. |

큐는 의도적으로 **bounded**입니다 — 폭주 시 메모리를 소진하기보다 감사 쓰기를 버립니다.
처리량에 맞게 크기를 잡으세요. [설정 레퍼런스](../reference/configuration.md#audit) 참고.

## 더 보기

- [관리자 콘솔 → Audit Logs](admin-console.md#audit-logs) — 검색 가능한 뷰어.
- [관리자 REST API](../reference/admin-api.md) — `audit-logs` 리소스.
