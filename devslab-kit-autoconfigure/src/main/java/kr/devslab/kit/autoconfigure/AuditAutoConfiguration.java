package kr.devslab.kit.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import kr.devslab.kit.audit.AuditEventPublisher;
import kr.devslab.kit.audit.core.repository.JpaPlatformAuditLogRepository;
import kr.devslab.kit.audit.core.service.AuditLogService;
import kr.devslab.kit.audit.core.service.DefaultAuditEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = {
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration"
})
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "devslab.kit.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditLogService auditLogService(JpaPlatformAuditLogRepository repository, ObjectMapper objectMapper) {
        return new AuditLogService(repository, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(
            AuditLogService auditLogService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return new DefaultAuditEventPublisher(auditLogService, applicationEventPublisher);
    }
}
