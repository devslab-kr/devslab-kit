package kr.devslab.kit.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application that verifies the devslab-kit starter end-to-end.
 *
 * <p>A plain {@code @SpringBootApplication} — the starter auto-registers its own
 * JPA entities and repositories (see {@code PersistenceAutoConfiguration}), so a
 * consumer needs no {@code @EntityScan} / {@code @EnableJpaRepositories} /
 * {@code scanBasePackages}, even from a package outside {@code kr.devslab.kit}.
 * (The external-consumer case is proven by {@code com.example.consumer}'s
 * {@code ExternalConsumerAutoRegistrationTests}.)
 *
 * <p>First-boot provisioning (a default tenant + admin user with the
 * {@code PLATFORM_ADMIN} role and the full {@code admin.*} permission set) is
 * handled by the starter's own bootstrap runner — see ADR 0001 and the
 * {@code devslab.kit.bootstrap.*} block in {@code application.yaml}.
 */
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
