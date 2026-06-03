# 캐시

**캐시**는 비싼 호출의 결과를 저장해, 다음에 같은 호출이 오면 다시 계산하지 않고 즉시
돌려줍니다. `devslab-kit`은 Spring의 `CacheManager` 뒤에 **플러그형 캐시**를 제공합니다:
**속성 하나**로 백엔드를 고르면 나머지는 kit이 엮습니다 — Redis용 JSON 직렬화 포함이라
`Serializable`을 구현하거나 직렬화기를 설정할 일이 없습니다. (배경: [ADR 0002](../adr/0002-distributed-cache.md).)

처음이면 [튜토리얼](../getting-started/tutorial.md)부터 — 이 가이드는 실행 중인 앱이 있다고
가정합니다.

## 백엔드

| `cache.type` | `CacheManager` | 용도 |
| --- | --- | --- |
| `in-memory` | `ConcurrentMapCacheManager` | 단일 노드 앱·로컬 개발(기본). |
| `redis` | `RedisCacheManager` | 여러 인스턴스 — 항목이 인스턴스 간 공유·일관. |
| `none` | `NoOpCacheManager` | 캐시 완전 비활성(매 조회 재계산). |

기준: **인스턴스가 하나일 동안은 `in-memory`**, 둘 이상 띄우면 `redis`로 바꿔 한 인스턴스가
캐시한 값을 다른 인스턴스도 보게 합니다.

## 설정

```yaml
# src/main/resources/application.yml
devslab:
  kit:
    cache:
      type: redis
      ttl: PT10M
      key-prefix: "myapp:"

# redis 백엔드는 Spring이 Redis를 가리키게도 해야 합니다:
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

`type: in-memory`(기본)면 `spring.data.redis` 블록은 필요 없습니다.

## 사용

표준 Spring 캐시이므로 `@Cacheable` / `@CacheEvict`와 주입된 `CacheManager`가 모두 동작합니다:

```java
// src/main/java/com/example/myapp/PriceService.java
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

@Service
class PriceService {

    @Cacheable("prices")                 // 미스일 때만 계산, 이후 캐시
    public Price lookup(String sku) {
        return expensiveLookup(sku);     // 예: 느린 DB·외부 호출
    }

    @CacheEvict(value = "prices", key = "#sku")   // 변경 시 항목 하나 제거
    public void priceChanged(String sku) {
        // 다음 lookup(sku)는 재계산 후 다시 캐시
    }
}
```

첫 `lookup("ABC")`는 메서드를 실행하고, 두 번째는 메서드 본문에 들어가지 않고 캐시된
`Price`를 돌려줍니다 — TTL이 만료되거나 `priceChanged("ABC")`가 비울 때까지.

!!! tip "정말 캐시되는지 확인"
    `expensiveLookup` 안에 로그를 찍으면 **첫 호출에만** 찍혀야 합니다. `redis`면 키가
    생기는 것도 볼 수 있습니다: `redis-cli KEYS 'myapp:prices*'`.

kit의 사용자별 [메뉴](menus.md) 트리도 이 같은 cache manager를 탑니다.

## Redis 직렬화

Redis 백엔드가 직렬화를 책임집니다: 값은 JSON으로 저장되고, 안전한 다형 타이핑은 allow-list
(`java.*` + `cache.allowed-package`, 기본 `kr.devslab`)로 제한됩니다. 즉:

- 캐시 타입에 `implements Serializable` 불필요,
- 등록할 직렬화기 빈 없음,
- `redis-cli`에서 값이 읽힘.

캐시 타입은 자신의 패키지에 두세요(또는 `cache.allowed-package`를 넓히세요).

## 튜닝

| 키 | 기본값 | |
| --- | --- | --- |
| `cache.type` | `in-memory` | 백엔드 선택. |
| `cache.ttl` | `PT10M` | 항목 TTL(Redis). |
| `cache.key-prefix` | `devslab:` | Redis 키 네임스페이스. |
| `cache.allowed-package` | `kr.devslab` | 다형 JSON 타이핑 allow-list. |

[설정 레퍼런스](../reference/configuration.md#cache) 참고.
