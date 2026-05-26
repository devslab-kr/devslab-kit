package kr.devslab.kit;

import org.springframework.boot.SpringApplication;

public class TestDevslabKitApplication {

    public static void main(String[] args) {
        SpringApplication.from(DevslabKitApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
