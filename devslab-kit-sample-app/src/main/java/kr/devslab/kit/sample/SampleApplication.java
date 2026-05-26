package kr.devslab.kit.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "kr.devslab.kit")
@AutoConfigurationPackage(basePackages = "kr.devslab.kit")
@EnableJpaRepositories(basePackages = "kr.devslab.kit")
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
