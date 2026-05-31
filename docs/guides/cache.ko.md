# 캐시

`devslab-kit`은 Spring의 `CacheManager` 뒤에 **플러그형 캐시**를 제공합니다. 속성 하나로
백엔드를 고르면 나머지는 kit이 배선합니다 — Redis용 JSON 직렬화 포함이라 `Serializable`
구현이나 직렬화기 설정이 필요 없습니다. (배경: [ADR 0002](../adr/0002-distributed-cache.md).)

## 백엔드

| `cache.type` | `CacheManager` | 용도 |
| --- | --- | --- |
| `in-memory` | `ConcurrentMapCacheManager` | 단일 노드 앱과 로컬 개발(기본값). |
| `redis` | `RedisCacheManager` | 여러 replica — 엔트리가 인스턴스 간 공유·일관됨. |
| `none` | `NoOpCacheManager` | 캐시 완전 비활성(모든 조회 재계산). |

```yaml
devslab:
  kit:
    cache:
      type: redis
      ttl: PT10M
      key-prefix: "myapp:"
```

`redis`를 쓸 때는 Spring도 Redis를 가리키게 하세요(`spring.data.redis.*`).

## 사용

표준 Spring 캐시이므로 `@Cacheable` / `@CacheEvict`와 주입된 `CacheManager`가 모두
동작합니다:

```java
@Service
class PriceService {

    @Cacheable("prices")
    public Price lookup(String sku) {
        // miss일 때만 계산; 설정된 백엔드 + TTL에 따라 캐시
        return expensiveLookup(sku);
    }
}
```

kit 자체의 사용자별 [메뉴](menus.md) 트리도 이 같은 캐시 매니저를 사용합니다.

## Redis 직렬화

Redis 백엔드가 직렬화를 책임집니다: 값은 JSON으로 저장되고, 안전한 다형 타이핑은
허용 목록(`java.*` + `cache.allowed-package`, 기본 `kr.devslab`)으로 제한됩니다. 따라서:

- 캐시 타입에 `implements Serializable` 불필요,
- 등록할 직렬화기 빈 불필요,
- `redis-cli`에서 읽을 수 있는 값.

캐시 타입은 자신의 패키지로 유지하세요(또는 `cache.allowed-package`를 넓히세요).

## 튜닝

| 키 | 기본값 | |
| --- | --- | --- |
| `cache.type` | `in-memory` | 백엔드 선택자. |
| `cache.ttl` | `PT10M` | 엔트리 TTL(Redis). |
| `cache.key-prefix` | `devslab:` | Redis 키 네임스페이스. |
| `cache.allowed-package` | `kr.devslab` | 다형 JSON 타이핑 허용 목록. |

[설정 레퍼런스](../reference/configuration.md#cache) 참고.
