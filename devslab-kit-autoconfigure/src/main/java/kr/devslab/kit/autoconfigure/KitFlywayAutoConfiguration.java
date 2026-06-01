package kr.devslab.kit.autoconfigure;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Runs devslab-kit's own Flyway migrations against a <strong>dedicated schema history
 * table</strong> ({@value #HISTORY_TABLE}) from a <strong>dedicated location</strong>
 * ({@value #LOCATION}), so the kit's migration line is fully independent of the consumer's.
 *
 * <p><strong>왜 분리하나.</strong> Flyway는 하나의 {@code flyway_schema_history} 테이블 안에서
 * 모든 마이그레이션을 단일 버전 라인으로 정렬·검증한다. kit이 기본 위치({@code db/migration})에
 * 마이그레이션을 실으면 consumer의 {@code V1__...sql}과 버전 번호가 충돌하고, kit이 다음 릴리스에서
 * 새 마이그레이션을 추가할 때 consumer가 이미 적용한 버전보다 작아져 out-of-order로 부팅이 깨진다.
 * 테이블 자체를 분리하면 두 라인이 완전히 독립되어 <em>kit도 V1, consumer도 V1부터</em> 시작할 수
 * 있고, kit이 버전을 올리며 마이그레이션을 추가해도 consumer 라인은 손대지 않는다.
 *
 * <p><strong>어떻게.</strong> Spring Boot의 자동 구성 {@code FlywayAutoConfiguration}은
 * {@code @ConditionalOnMissingBean(Flyway.class)}로 {@code flyway} 빈을 만든다. 따라서 여기서
 * {@link Flyway} 타입의 빈을 등록하면 consumer를 위한 기본 Flyway가 backoff되어 사라진다. 그래서
 * kit은 {@link Flyway} 빈을 등록하지 <em>않고</em> {@link DevslabKitFlywayMigrator} 안에서
 * 프로그램적으로 {@code migrate()}만 호출한다. 결과적으로:
 * <ul>
 *   <li>kit → {@value #LOCATION} 를 {@value #HISTORY_TABLE} 테이블로 (이 클래스)</li>
 *   <li>consumer → 기본 {@code classpath:db/migration} 를 기본 {@code flyway_schema_history}
 *       테이블로 (Spring Boot 자동 구성 그대로, consumer의 {@code spring.flyway.*} 설정도 그대로)</li>
 * </ul>
 *
 * <p>두 Flyway는 같은 스키마를 공유하므로 실행 순서가 중요하다. consumer의 기본 Flyway를 먼저
 * (보통 빈 스키마에서) 돌리고 kit을 두 번째로 돌리도록 {@link #devslabKitFlywayOrderingPostProcessor()}로
 * 순서를 고정한다. kit이 두 번째로 돌 때는 스키마가 이미 비어있지 않으므로
 * {@code baselineOnMigrate(true)} + {@code baselineVersion("0")} 으로 kit의 첫 마이그레이션(V1)을
 * 건너뛰지 않고 자기 history 테이블만 baseline 한다. JPA의 {@code EntityManagerFactory}는
 * {@link EntityManagerFactoryDependsOnPostProcessor}로 이 마이그레이터에 의존하게 만들어,
 * {@code ddl-auto=validate}가 kit 테이블 생성 이후에 검증하도록 보장한다.
 *
 * <p><strong>업그레이드 주의.</strong> 0.2.x에서 kit 마이그레이션을 기본 {@code flyway_schema_history}로
 * 이미 적용한 데이터베이스를 0.3.0으로 올리면, 새 {@value #HISTORY_TABLE} 테이블이 비어 있어 kit이
 * 이미 존재하는 {@code platform_*} 테이블을 다시 만들려다 실패할 수 있다. pre-1.0 단계에서 실제
 * 영속 DB는 데모/로컬뿐이므로 해당 DB는 재생성하면 된다(자세한 내용은 CHANGELOG 참고).
 */
@AutoConfiguration(
        afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        beforeName = {
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        })
@ConditionalOnClass(Flyway.class)
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class KitFlywayAutoConfiguration {

    /** kit 전용 마이그레이션 위치(각 {@code -core} jar에 동봉). */
    static final String LOCATION = "classpath:db/devslab-kit";

    /** kit 전용 스키마 히스토리 테이블(기본 {@code flyway_schema_history}와 분리). */
    static final String HISTORY_TABLE = "devslab_kit_schema_history";

    /** 마이그레이터 빈 이름 — JPA depends-on 배선에서 참조. */
    static final String MIGRATOR_BEAN_NAME = "devslabKitFlywayMigrator";

    /** Spring Boot가 만드는 consumer용 기본 Flyway 마이그레이션 initializer 빈 이름. */
    static final String CONSUMER_FLYWAY_INITIALIZER = "flywayInitializer";

    @Bean(MIGRATOR_BEAN_NAME)
    DevslabKitFlywayMigrator devslabKitFlywayMigrator(DataSource dataSource) {
        return new DevslabKitFlywayMigrator(dataSource);
    }

    /**
     * kit 마이그레이터가 consumer의 기본 Flyway initializer <em>뒤에</em> 돌도록 의존 관계를 추가한다.
     *
     * <p>두 Flyway가 같은 스키마를 공유하므로, 먼저 도는 쪽이 스키마를 채우면 나중에 도는 쪽은
     * "비어있지 않은 스키마 + 자기 history 테이블 없음"을 보게 된다. kit 마이그레이터는
     * {@code baselineOnMigrate}로 이를 견디지만, consumer의 기본 Flyway는 그렇지 않다(그리고 그 설정을
     * 강제로 바꾸면 consumer의 V1을 건너뛰는 부작용이 있어 건드리지 않는다). 따라서 consumer가 먼저
     * (보통 빈 스키마에서) 돌고 kit이 두 번째로 돌게 순서를 고정한다. {@code flywayInitializer} 빈이
     * 있을 때만 배선하므로(존재 검사), Flyway 설정이 다른 consumer에서도 안전하다.
     */
    @Bean
    static BeanFactoryPostProcessor devslabKitFlywayOrderingPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition(MIGRATOR_BEAN_NAME)
                    && beanFactory.containsBeanDefinition(CONSUMER_FLYWAY_INITIALIZER)) {
                BeanDefinition migrator = beanFactory.getBeanDefinition(MIGRATOR_BEAN_NAME);
                migrator.setDependsOn(
                        StringUtils.addStringToArray(migrator.getDependsOn(), CONSUMER_FLYWAY_INITIALIZER));
            }
        };
    }

    /**
     * JPA가 클래스패스에 있을 때만, {@code EntityManagerFactory}가 kit 마이그레이터 이후에
     * 만들어지도록 의존 관계를 추가한다({@code ddl-auto=validate}가 kit 테이블을 보게).
     * PP 클래스 참조를 이 중첩 구성에 격리해, JPA 미사용 consumer에서 클래스 로딩이 깨지지 않게 한다.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EntityManagerFactory.class)
    static class JpaDependsOnConfiguration {

        @Bean
        static EntityManagerFactoryDependsOnPostProcessor devslabKitFlywayJpaDependsOnPostProcessor() {
            return new EntityManagerFactoryDependsOnPostProcessor(MIGRATOR_BEAN_NAME);
        }
    }

    /**
     * kit 마이그레이션을 전용 위치/테이블에 적용한다. {@link Flyway} 타입의 Spring 빈이 아니라
     * 일반 빈이라, Spring Boot의 consumer용 자동 Flyway({@code @ConditionalOnMissingBean(Flyway.class)})를
     * 죽이지 않는다.
     */
    static final class DevslabKitFlywayMigrator implements InitializingBean {

        private final DataSource dataSource;

        DevslabKitFlywayMigrator(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void afterPropertiesSet() {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations(LOCATION)
                    .table(HISTORY_TABLE)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
        }
    }
}
