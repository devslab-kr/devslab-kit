# 감사 로깅

kit은 감사 추적을 **비동기**로 기록하므로, 로깅이 요청의 임계 경로에 끼어들지 않습니다.
이벤트는 메타데이터를 JSONB로 하여 PostgreSQL에 영속화됩니다.

## 이벤트 발행

`AuditEventPublisher`를 주입하고 `AuditEvent`를 `publish`하세요. `AuditEvent`는 **actor**,
**action**, **target**, **outcome**, 타임스탬프, 그리고 자유 형식 **metadata** 맵을
담습니다. `AuditEvent.builder()`로 만듭니다:

```java
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;

@Service
class TenantAdminService {
    private final AuditEventPublisher audit;

    TenantAdminService(AuditEventPublisher audit) { this.audit = audit; }

    void suspend(String tenantId, String reason) {
        // … 변경 수행 …
        audit.publish(
            AuditEvent.builder()
                // action / target / outcome / metadata — AuditEvent 빌더 참고
                .build());
    }
}
```

actor와 타임스탬프는 생략하면 현재 컨텍스트에서 채워지고, 발행기는 이벤트를 비동기
리스너에 넘겨 기록합니다.

## 추적 조회

감사 로그는 관리자 API(`audit-logs`)로 검색·필터합니다 — actor, action, target type,
outcome, 기간 기준. [관리자 REST API](../reference/admin-api.md) 참고.
[관리자 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)은 상세 드로어가 딸린
검색 가능한 감사 로그 뷰를 제공합니다.

## 튜닝

| 키 | 기본값 | |
| --- | --- | --- |
| `audit.enabled` | `true` | 필요 없으면 서브시스템 전체를 끔. |
| `audit.async-queue-capacity` | `1024` | 비동기 라이터에 공급하는 bounded 큐. |

큐는 의도적으로 **bounded**입니다 — 폭주 시 메모리를 소진하는 대신 감사 쓰기를 흘려보냅니다.
처리량에 맞게 크기를 정하세요. [설정 레퍼런스](../reference/configuration.md#audit) 참고.
