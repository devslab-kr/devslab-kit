package com.example.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * devslab-kit 0.3.0의 Flyway history 테이블 분리를 검증한다 — 그리고 분리 이전이라면 부팅 자체가
 * 깨졌을 회귀를 가드한다.
 *
 * <p>이 테스트가 도는 sample-app은 자기 {@code db/migration/V1__sample_app_note.sql}을 가진다.
 * 그 V1은 kit의 V1({@code platform_user_account})과 번호가 같다. kit이 자기 마이그레이션을 전용
 * {@code devslab_kit_schema_history} 테이블로 분리했기 때문에 두 V1은 충돌 없이 공존한다. 분리가
 * 없었다면 컨텍스트 로딩이 {@code FlywayException: Found more than one migration with version 1}로
 * 실패했을 것이다(= bookrecord가 실제로 맞았던 버그).
 *
 * <ul>
 *   <li>두 history 테이블이 모두 존재하고 분리되어 있다,</li>
 *   <li>kit 테이블과 consumer 테이블이 모두 만들어진다,</li>
 *   <li>두 라인이 각자 독립적으로 V1부터 시작한다(같은 '1' 버전이 서로 다른 테이블에).</li>
 * </ul>
 *
 * <p>{@link ExternalConsumerAutoRegistrationTests}와 동일한 프로퍼티를 써서 Spring TestContext
 * 캐시(같은 Testcontainers Postgres)를 공유한다.
 */
@Import(ConsumerTestcontainers.class)
@SpringBootTest
@TestPropertySource(properties = {
        "devslab.kit.bootstrap.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
class FlywayHistorySeparationTests {

    @Autowired
    private JdbcTemplate jdbc;

    private boolean tableExists(String name) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, name);
        return count != null && count > 0;
    }

    private int rows(String sql) {
        Integer count = jdbc.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    void kitAndConsumerUseSeparateHistoryTables() {
        assertThat(tableExists("flyway_schema_history")).as("consumer 기본 history").isTrue();
        assertThat(tableExists("devslab_kit_schema_history")).as("kit 전용 history").isTrue();
    }

    @Test
    void bothKitAndConsumerTablesAreCreated() {
        assertThat(tableExists("platform_user_account")).as("kit 마이그레이션이 만든 테이블").isTrue();
        assertThat(tableExists("sample_app_note")).as("consumer 마이그레이션이 만든 테이블").isTrue();
    }

    @Test
    void eachLineStartsAtVersionOneIndependently() {
        // consumer 라인의 V1 = sample_app_note, kit 라인의 V1 = platform_user_account.
        // 같은 '1' 버전이 서로 다른 history 테이블에 공존 = 분리 성공.
        assertThat(rows("SELECT count(*) FROM flyway_schema_history WHERE version = '1'"))
                .as("consumer V1").isEqualTo(1);
        assertThat(rows("SELECT count(*) FROM devslab_kit_schema_history WHERE version = '1'"))
                .as("kit V1").isEqualTo(1);
        assertThat(rows("SELECT count(*) FROM devslab_kit_schema_history WHERE version = '11'"))
                .as("kit V11 (kit 라인이 독립적으로 11까지 적용됨)").isEqualTo(1);
    }
}
