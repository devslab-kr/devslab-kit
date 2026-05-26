package kr.devslab.kit.autoconfigure;

import kr.devslab.kit.access.core.entity.PlatformGroupEntity;
import kr.devslab.kit.access.core.entity.PlatformGroupRoleEntity;
import kr.devslab.kit.access.core.entity.PlatformPermissionEntity;
import kr.devslab.kit.access.core.entity.PlatformRoleEntity;
import kr.devslab.kit.access.core.entity.PlatformRolePermissionEntity;
import kr.devslab.kit.access.core.entity.PlatformUserGroupEntity;
import kr.devslab.kit.access.core.entity.PlatformUserRoleEntity;
import kr.devslab.kit.audit.core.entity.PlatformAuditLogEntity;
import kr.devslab.kit.identity.core.entity.PlatformUserAccountEntity;
import kr.devslab.kit.identity.event.LoginFailedEvent;
import kr.devslab.kit.identity.event.LoginSucceededEvent;
import kr.devslab.kit.identity.event.UserAccountCreatedEvent;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native reflection / resource hints for the devslab-kit
 * starter beans that depend on runtime reflection (JPA entities, event
 * payloads serialised by Spring, Flyway migration resources).
 *
 * <p>Registered via {@code @ImportRuntimeHints} on
 * {@link TenantAutoConfiguration} so it always loads alongside the kit.
 * Planning doc §14 calls this out as a first-class requirement.
 */
public class DevslabKitRuntimeHints implements RuntimeHintsRegistrar {

    private static final Class<?>[] JPA_ENTITIES = {
            PlatformUserAccountEntity.class,
            PlatformRoleEntity.class,
            PlatformPermissionEntity.class,
            PlatformUserRoleEntity.class,
            PlatformRolePermissionEntity.class,
            PlatformGroupEntity.class,
            PlatformUserGroupEntity.class,
            PlatformGroupRoleEntity.class,
            PlatformMenuEntity.class,
            PlatformAuditLogEntity.class,
    };

    private static final Class<?>[] EVENT_PAYLOADS = {
            LoginSucceededEvent.class,
            LoginFailedEvent.class,
            UserAccountCreatedEvent.class,
    };

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // JPA entities — Hibernate needs constructors + declared methods + declared fields at runtime
        for (Class<?> entity : JPA_ENTITIES) {
            hints.reflection().registerType(entity, MemberCategory.values());
        }

        // ApplicationEvent payload records — listener invocation reflects on accessor methods
        for (Class<?> event : EVENT_PAYLOADS) {
            hints.reflection().registerType(event,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        }

        // Flyway migrations ship inside each -core jar
        hints.resources().registerPattern("db/migration/*.sql");

        // Spring Boot AutoConfiguration imports file (defensive — Spring usually handles this)
        hints.resources().registerPattern(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
    }
}
