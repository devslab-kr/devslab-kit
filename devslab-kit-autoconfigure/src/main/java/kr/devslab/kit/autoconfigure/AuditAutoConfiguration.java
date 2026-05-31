package kr.devslab.kit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.concurrent.Executor;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import kr.devslab.kit.audit.core.service.AuditLogService;
import kr.devslab.kit.audit.core.service.DefaultAuditEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@AutoConfiguration(afterName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration"
})
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(DevslabKitProperties.class)
public class AuditAutoConfiguration {

    static final String AUDIT_PERSISTENCE_EXECUTOR = "devslabKitAuditPersistenceExecutor";

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogService auditLogService(JpaPlatformAuditLogRepository repository, ObjectMapper objectMapper) {
        return new AuditLogService(repository, objectMapper);
    }

    /**
     * Single-threaded executor with a bounded queue + CallerRunsPolicy. One
     * worker is enough — audit writes are tiny and we don't want them
     * starving the main pool. The queue size is taken from
     * {@code devslab.kit.audit.async-queue-capacity} (default 1024).
     */
    @Bean(name = AUDIT_PERSISTENCE_EXECUTOR)
    @ConditionalOnMissingBean(name = AUDIT_PERSISTENCE_EXECUTOR)
    public Executor auditPersistenceExecutor(DevslabKitProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(properties.getAudit().getAsyncQueueCapacity());
        executor.setThreadNamePrefix("devslab-kit-audit-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(
            AuditLogService auditLogService,
            ApplicationEventPublisher applicationEventPublisher,
            @Qualifier(AUDIT_PERSISTENCE_EXECUTOR) Executor persistenceExecutor
    ) {
        return new DefaultAuditEventPublisher(auditLogService, applicationEventPublisher, persistenceExecutor);
    }

    /**
     * Bridges identity login events onto the audit log. Registered here so
     * it only activates when auditing is enabled and an
     * {@link AuditEventPublisher} exists.
     */
    @Bean
    @ConditionalOnMissingBean
    public LoginAuditBridge loginAuditBridge(AuditEventPublisher auditEventPublisher) {
        return new LoginAuditBridge(auditEventPublisher);
    }
}
