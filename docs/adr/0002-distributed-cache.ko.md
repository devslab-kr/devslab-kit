# ADR 0002 — 끼울 수 있는 분산 캐시 (property 한 줄, 직렬화 고민 0)

- **상태:** 수락(Accepted)
- **날짜:** 2026-05-31 (2026-05-31 수락)
- **언어:** [English](0002-distributed-cache.md) · [한국어](0002-distributed-cache.ko.md)
- **구현:** `devslab-kit-cache-api` + `devslab-kit-cache-core` —
  `CacheProperties`(`devslab.kit.cache.*`), `CacheAutoConfiguration`(`type`
  스위치 + 가드된 `@EnableCaching`, none/in-memory 백엔드). Redis 백엔드 + JSON
  직렬화(PR 2), 메뉴 캐시 마이그레이션(PR 3), sample-app/문서(PR 4)는 아래 구현
  계획대로 이어진다.

## 배경

`devslab-kit`은 이미 한 가지를 캐시한다 — 사용자별 메뉴 트리를, 직접 만든 작은
`CachingMenuProvider`(`ConcurrentHashMap` 기반)로. 단일 인스턴스에선 괜찮지만,
consumer가 replica를 둘 이상 띄우는 순간 깨진다: 인스턴스마다 자기 map을 가지므로,
메뉴 수정(또는 캐시된 어떤 값이든)이 그 쓰기를 처리한 노드 외 모든 노드에서 각자의
TTL이 만료될 때까지 stale로 남는다.

공개 첫 릴리스를 준비하며 드러난 더 본질적인 지점: **Spring Boot에서 올바른 분산
캐시를 세팅하는 건 번거롭고, 모든 consumer가 같은 세금을 낸다.** `CacheManager`를
고르고, `RedisConnectionFactory`를 엮고, 그리고 — 모두를 무는 부분 — 직렬화를
설정해야 한다. 기본 JDK 직렬화는 캐시할 모든 타입이 `Serializable`이어야 하고,
불투명한 바이너리를 만들며, record나 marker 인터페이스를 깜빡한 타입을 처음 캐시하는
순간 런타임에 `SerializationException`을 던진다. JSON으로 바꾸려면
`GenericJackson2JsonRedisSerializer`를 손수 설정하고, 다형 값이 round-trip 되도록
default-typing을 정하고, `ObjectMapper`를 제대로 맞춰야 한다.

플랫폼 스타터는 그 결정을 **한 번** 내려 어떤 하위 제품도 다시는 안 하게 만들기에
딱 맞는 자리다. 목표:

> consumer가 **property 한 줄**로 캐시를 인메모리→분산(Redis)으로 바꾸고,
> **직렬화는 절대 안 건든다.** 자기 `@Cacheable` 메서드가 replica 간에 그냥 동작한다.

## 결정

### 1. `type` 스위치를 가진 `devslab.kit.cache.*` 블록

```yaml
devslab.kit.cache:
  type: in-memory        # none | in-memory | redis   (기본: in-memory)
  ttl: PT10M             # 기본 엔트리 TTL
  key-prefix: "devslab:" # 공유 Redis에서 키 네임스페이스
  null-values: false     # null 캐시? (기본 no — miss 가리는 것 방지)
```

- **`in-memory`** (기본) — `ConcurrentMapCacheManager` 계열. 외부 의존성 없음;
  지금과 같은 단일 인스턴스 동작.
- **`redis`** — JSON 직렬화가 kit에 의해 완전히 사전 설정된 `RedisCacheManager`
  (§3 참고). consumer는 Redis 연결 설정(`spring.data.redis.*`)만 추가하면 끝.
- **`none`** — `NoOpCacheManager`. 캐싱 애너테이션이 pass-through가 됨; 쓰기를
  즉시 봐야 하는 테스트에 유용.

### 2. kit이 `CacheManager` 빈을 조건부로 소유

새 `CacheAutoConfiguration`이 `devslab.kit.cache.type`에 따라 `CacheManager`를
제공하되, 각 빈은 backing 클래스가 있을 때만 활성화되도록 가드:

- `redis`는 classpath에 `RedisConnectionFactory`(`@ConditionalOnClass`) **그리고**
  connection factory 빈(`@ConditionalOnBean`)을 요구. `type=redis`인데 Redis가 안
  엮였으면, 조용히 인메모리로 격하되는 대신 명확한 메시지와 함께 빠르게 실패.
- 모든 빈은 `@ConditionalOnMissingBean(CacheManager.class)` — 자기 `CacheManager`를
  정의한 consumer가 항상 이긴다. kit은 명시적 선택과 싸우지 않는다.
- `@EnableCaching`은 kit이 켜서 consumer가 기억 안 해도 되게 한다(역시
  `@ConditionalOnMissingBean(CacheManager.class)` 가드라, consumer가 직접 캐싱을
  관리하면 비활성).

### 3. 직렬화는 kit 안에서 한 번에 해결

`redis` 경로에서 kit은 `RedisCacheConfiguration`을 이렇게 설정:

- **값:** kit 소유 `ObjectMapper` 위의 `GenericJackson2JsonRedisSerializer` —
  JSON, `redis-cli`에서 사람이 읽을 수 있고, `Serializable` 불필요. default-typing은
  **안전한** 형태로 활성화(CVE 위험이 있는 `LaissezFaireSubTypeValidator`가 아니라
  allow-list validator로 non-final 타입에 제한)해서, consumer가 신경 안 써도 다형·
  제네릭 값이 round-trip 된다.
- **키:** 설정된 `key-prefix`를 쓰는 `StringRedisSerializer`.
- **TTL & null 처리:** property 블록에서.

이게 가치 제안의 핵심이다: consumer가 Java record를 캐시하고 다른 노드에서 같은
record로 읽어오며, `SerializationException`도 직렬화 설정 한 줄도 절대 안 본다.

### 4. 모듈 배치

기존 `-api` / `-core` 분리를 따른다:

- **`devslab-kit-cache-api`** — 작은 계약: `CacheNames` 상수 홀더, 그리고 안정적으로
  두고 싶은 kit 수준 캐시 추상화. 최소한; 대부분 consumer는 Spring `@Cacheable`만 씀.
- **`devslab-kit-cache-core`** — `CacheAutoConfiguration`은
  `devslab-kit-autoconfigure`(다른 auto-config들이 있는 곳)에 두되, Redis 전용 설정
  + `ObjectMapper`/직렬화 wiring은 여기 둬서 Redis 클래스를 선택적·격리된 의존성으로.

Spring의 `spring-boot-starter-data-redis`는 `-cache-core`에 `optional`/`compileOnly`로
선언해, consumer가 opt-in 할 때만 classpath에 오른다. 스타터
(`devslab-kit-spring-boot-starter`)는 `-cache-core`를 끌어오지만, Redis 자체는
transitive 의존성이 **아니다** — consumer가 `type=redis`로 둘 때
`spring-boot-starter-data-redis`를 추가한다. README가 이 한 줄을 문서화한다.

### 5. 메뉴 캐시가 첫 소비자가 됨

기존 `CachingMenuProvider`를 공유 캐시 위에 다시 표현한다:

- `type=in-memory`/`redis` → 메뉴 provider가 자기 private map 대신 kit
  `CacheManager`에 위임(`menu:<userId>` 키에 `@Cacheable` 식 읽기). 캐시 스토리는
  둘이 아니라 하나.
- `type=none` → 메뉴 provider가 캐싱을 건너뜀. 오늘의 "0/음수 TTL이면 데코레이터
  비활성" 특수 케이스를 대체.
- `devslab.kit.menu.cache-ttl`은 선택적 **per-cache 오버라이드**로 유지; 미설정 시
  `devslab.kit.cache.ttl`을 상속. 기존 consumer는 기본값에서 동작 변화 없음.

### 6. consumer가 공짜로 얻는 것

kit이 `@EnableCaching`을 켜고 올바르게 직렬화하는 `CacheManager`를 소유하므로,
consumer **자기** 코드가 메서드에 `@Cacheable("their-cache")`를 붙이면 추가 설정
없이 즉시 분산 캐싱을 얻는다 — 대표 혜택. 자기 도메인 타입이 kit의 직렬화 정책으로,
replica 간에, JSON으로 캐시된다.

## 결과(Consequences)

**긍정**
- property 한 줄(`type: redis`)이 단일 노드 캐시를 올바른 분산 캐시로 바꾼다.
  직렬화 설정도, `Serializable`도, 영원히 없음.
- 메뉴 캐시가 multi-replica 정합성 버그이길 멈춘다.
- 혜택이 consumer 자신의 `@Cacheable` 사용으로 확장된다 — 내부 최적화가 아니라
  진짜 플랫폼 기능.
- Redis는 선택적·non-transitive로 유지; 인메모리 기본이 단순 배포의 무의존성
  스토리를 지킨다.

**부정 / 비용**
- 새 모듈 + auto-config + 신중히 설정한 `ObjectMapper`. default-typing은 안전한
  validator로 해야 함 — 보안을 제대로 잡아야 하는 유일한 지점
  (`LaissezFaireSubTypeValidator` 금지).
- 메뉴를 Spring `CacheManager`로 캐시하면 맞춤 `invalidate(userId)` 메서드를 잃는다
  (동등물을 노출하지 않는 한); admin 메뉴 수정은 문서화된 eviction 경로
  (`@CacheEvict` 또는 `Cache.evict` 호출)가 필요.
- 스타터가 기본으로 켜는 게 하나 더(`@EnableCaching`); `@ConditionalOnMissingBean`
  가드지만 짚어둘 가치 있음.

## 구현 계획 (PR 분할)

1. **`-cache-api` + `-cache-core` + `CacheAutoConfiguration`** — `type` 스위치,
   in-memory + none 매니저, property 블록, 가드된 `@EnableCaching`. Redis는 아직
   없음. 조건부 wiring 단위 테스트.
2. **Redis 경로 + 직렬화** — `RedisCacheManager`, 안전한 default-typing
   `ObjectMapper`, key prefix/TTL. record가 새 `CacheManager`를 거쳐 round-trip
   됨을 증명하는 실 Redis Testcontainers 테스트(직렬화가 리스크).
3. **메뉴 캐시 마이그레이션** — 공유 `CacheManager` 위로; per-cache TTL 오버라이드
   유지; admin eviction 보존. 세 `type` 값 전부에서 메뉴 캐싱 회귀 테스트.
4. **sample-app + 문서** — sample-app이 이미 띄우는 compose Redis로 `type=redis`를
   보여줌(그래서 Redis가 죽은 짐이길 멈춤); README가 property 한 줄 전환 +
   "당신의 @Cacheable이 그냥 된다" 혜택을 문서화.

## 검토한 대안

- **직접 만든 map 유지.** 기각 — replica 2개 이상에서 깨지고, 모든 consumer가 분산
  캐싱을 스스로 다시 푼다(이 kit이 없애려는 바로 그 세금).
- **인메모리는 Caffeine, 분산은 Redis, 두 코드 경로.** v1에선 기각 — Spring
  `CacheManager` 추상화가 이미 한 인터페이스 뒤로 backend를 바꾸게 해주고,
  in-memory 기본엔 `ConcurrentMapCacheManager`로 충분. Caffeine은 나중에 같은 `type`
  스위치 뒤로(`type: caffeine`) API 변경 없이 끼울 수 있음.
- **Redis를 hard transitive 의존성으로.** 기각 — 인메모리 기본만 원하는 consumer에게
  Redis jar(와 Redis 서버 기대)를 강요. 선택적 유지가 단순 경로를 보존.
- **consumer가 자기 직렬화를 설정하게.** 기각 — 그게 바로 이 ADR이 없애는 고통.
  여기서 kit의 가치는 그걸 한 번에 정하는 것.
