# 로드맵

`devslab-kit`이 향하는 방향에 대한 대략적이고 비구속적인 그림입니다. 우선순위는 실제 사용에
따라 바뀝니다 — [이슈를 열어](https://github.com/devslab-kr/devslab-kit/issues) 의견을 주세요.

## 0.1.0 — 첫 공개 릴리스

현재 초점: 이미 기능이 완성된 플랫폼을 출시하는 것.

- Identity, Access(RBAC + 그룹 + ABAC), 멀티테넌시, 동적 메뉴, 감사, 플러그형 캐시,
  최초 관리자 부트스트랩, 관리자 REST API.
- Maven Central 배포 + 이 문서 사이트.
- 동반 [관리자 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui).

## 0.1.0 이후 (후보)

- **GraalVM native** — 샘플 앱이 `nativeCompile`을 수행 중; 검증되고 문서화된 경로로 승격.
- **선택형 adapter starter** — 설계상 core 밖에 두고 필요 시 추가: WebFlux, GraphQL,
  RabbitMQ, Spring Session, OAuth2(client / resource server).
- **테넌트 리졸버 / 캐시 백엔드 추가** — 실제 소비자가 필요로 할 때
  ("pull, don't push" 원칙 — 두 번째 소비자가 생기면 추출).
- **하드닝** — 통합 커버리지 확대, 보안 리뷰, 성능 개선.

## 0.1.0 이후 출시됨

- **환경 간 설정 동기화** (`0.4.0`) — 정의성 설정(권한·역할·메뉴; 옵트인 사용자)을 코드 기준
  번들로 export/import, `merge`/`mirror`, 기본 dry-run. [가이드](guides/config-sync.ko.md) 와
  [ADR 0003](adr/0003-config-sync.ko.md) 참고.

## 버전 정책

라이브러리 메이저는 Spring Boot 메이저를 따릅니다: **`4.x.y`는 Spring Boot 4.x 대상**.
Spring Boot 4.1 라인은 자체 `4.1` 정렬 릴리스로 나옵니다. 실제 출시 내역은
[변경 이력](changelog.md)을 참고하세요.
