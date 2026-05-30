package kr.devslab.kit.sample;

import kr.devslab.kit.access.core.repository.JpaPlatformPermissionRepository;
import kr.devslab.kit.access.core.repository.JpaPlatformRoleRepository;
import kr.devslab.kit.access.core.service.PermissionAdminService;
import kr.devslab.kit.access.core.service.RoleAdminService;
import kr.devslab.kit.access.core.service.RolePermissionService;
import kr.devslab.kit.access.core.service.UserRoleService;
import kr.devslab.kit.identity.core.repository.JpaPlatformUserAccountRepository;
import kr.devslab.kit.identity.core.service.PlatformUserAccountAdminService;
import kr.devslab.kit.tenant.TenantService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "kr.devslab.kit")
@AutoConfigurationPackage(basePackages = "kr.devslab.kit")
@EnableJpaRepositories(basePackages = "kr.devslab.kit")
@EnableConfigurationProperties(SampleSeedProperties.class)
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    @Bean
    SampleSeedRunner sampleSeedRunner(
            SampleSeedProperties props,
            TenantService tenantService,
            PermissionAdminService permissionAdminService,
            JpaPlatformPermissionRepository permissionRepository,
            RoleAdminService roleAdminService,
            JpaPlatformRoleRepository roleRepository,
            RolePermissionService rolePermissionService,
            PlatformUserAccountAdminService userAdminService,
            JpaPlatformUserAccountRepository userRepository,
            UserRoleService userRoleService
    ) {
        return new SampleSeedRunner(
                props,
                tenantService,
                permissionAdminService,
                permissionRepository,
                roleAdminService,
                roleRepository,
                rolePermissionService,
                userAdminService,
                userRepository,
                userRoleService
        );
    }
}
