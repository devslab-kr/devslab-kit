package kr.devslab.kit.sample;

import org.springframework.boot.SpringApplication;

public class TestSampleApplication {

    public static void main(String[] args) {
        SpringApplication.from(SampleApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
