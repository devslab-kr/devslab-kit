package kr.devslab.kit.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Sample application that verifies the devslab-kit starter end-to-end.
 *
 * <p>First-boot provisioning (a default tenant + admin user with the
 * {@code PLATFORM_ADMIN} role and the full {@code admin.*} permission set) is
 * handled by the starter's own bootstrap runner — see ADR 0001 and the
 * {@code devslab.kit.bootstrap.*} block in {@code application.yaml}. The sample
 * app no longer ships its own seed runner.
 */
@SpringBootApplication(scanBasePackages = "kr.devslab.kit")
@AutoConfigurationPackage(basePackages = "kr.devslab.kit")
@EnableJpaRepositories(basePackages = "kr.devslab.kit")
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
