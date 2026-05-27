package kr.devslab.kit.audit.core.service;

import java.util.concurrent.Executor;
import kr.devslab.kit.audit.AuditEvent;
import kr.devslab.kit.audit.AuditEventPublisher;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Default {@link AuditEventPublisher}.
 *
 * <p>Publishing an event has two sides:
 *
 * <ul>
 *   <li>The persistence write goes through {@link AuditLogService#record}
 *       on a bounded background {@link Executor}, so request threads return
 *       without waiting for the DB round-trip.</li>
 *   <li>The Spring {@link ApplicationEventPublisher} fanout stays
 *       synchronous, so {@code @EventListener}s on the request thread
 *       (e.g. metrics counters) see the event before the request returns.</li>
 * </ul>
 *
 * <p>Backpressure: the executor is configured by {@code AuditAutoConfiguration}
 * with a single worker thread + a bounded LinkedBlockingQueue + a
 * {@code CallerRunsPolicy}. When the queue is saturated the caller runs the
 * persistence write inline — slow, but no audit row is ever lost.
 */
public class DefaultAuditEventPublisher implements AuditEventPublisher {

    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Executor persistenceExecutor;

    public DefaultAuditEventPublisher(
            AuditLogService auditLogService,
            ApplicationEventPublisher applicationEventPublisher,
            Executor persistenceExecutor
    ) {
        this.auditLogService = auditLogService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.persistenceExecutor = persistenceExecutor;
    }

    @Override
    public void publish(AuditEvent event) {
        persistenceExecutor.execute(() -> auditLogService.record(event));
        applicationEventPublisher.publishEvent(event);
    }
}
