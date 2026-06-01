-- Consumer(sample-app) 자체 Flyway 마이그레이션.
--
-- 이 V1은 kit의 V1(platform_user_account)과 "번호가 같다". kit이 0.3.0부터 자기 마이그레이션을
-- 전용 history 테이블(devslab_kit_schema_history)로 분리했기 때문에, consumer는 기본
-- flyway_schema_history에서 아무 충돌 없이 V1부터 시작할 수 있다. 분리 전이라면 이 파일만으로도
-- "Found more than one migration with version 1" 로 부팅이 깨졌다 — FlywayHistorySeparationTests가
-- 바로 그 회귀를 가드한다.
CREATE TABLE sample_app_note (
    id         UUID         NOT NULL PRIMARY KEY,
    note       VARCHAR(280) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
