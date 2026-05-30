package kr.devslab.kit.admin.policy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.devslab.kit.access.core.service.DefaultPolicyEvaluator;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import kr.devslab.kit.access.policy.PolicyEvaluation;
import kr.devslab.kit.access.policy.PolicyEvaluator;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only admin surface for the ABAC {@link PolicyEvaluator}.
 *
 * <p>Two endpoints:
 *
 * <ul>
 *   <li>{@code GET /admin/api/v1/policies} — list registered policies
 *       (name + optional description) for the admin UI's policy table.</li>
 *   <li>{@code POST /admin/api/v1/policies/test} — dry-run a
 *       {@code (subject, action, resource)} tuple through the evaluator
 *       and report the resulting effect.</li>
 * </ul>
 */
@RestController
@RequestMapping(AdminApiPaths.BASE + "/policies")
public class PolicyAdminController {

    private final PolicyEvaluator evaluator;

    public PolicyAdminController(PolicyEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @GetMapping
    public List<PolicyDescriptor> list() {
        if (evaluator instanceof DefaultPolicyEvaluator def) {
            return def.registeredPolicies().stream()
                    .map(p -> new PolicyDescriptor(p.name(), p.description()))
                    .sorted(Comparator.comparing(PolicyDescriptor::name))
                    .toList();
        }
        return List.of();
    }

    @PostMapping("/test")
    public PolicyTestResponse test(@Valid @RequestBody PolicyTestRequest req) {
        PolicyContext ctx = PolicyContext.builder()
                .user(req.subject() != null && req.subject().userId() != null
                        ? UserId.of(req.subject().userId()) : null)
                .tenant(req.subject() != null && req.subject().tenantId() != null
                        ? TenantId.of(req.subject().tenantId()) : null)
                .resource(
                        req.resource() == null ? null : req.resource().type(),
                        req.resource() == null ? null : req.resource().id())
                .resourceAttributes(req.resource() == null ? Map.of()
                        : Optional.ofNullable(req.resource().attributes()).orElseGet(Map::of))
                .environmentAttributes(Optional.ofNullable(req.environment()).orElseGet(Map::of))
                .build();
        PolicyEvaluation result = evaluator.evaluateDetailed(req.policyName(), ctx);
        return new PolicyTestResponse(result.decision(), result.reason(), result.matchedRules());
    }

    public record PolicyDescriptor(String name, String description) {
    }

    public record PolicyTestRequest(
            @NotBlank String policyName,
            Subject subject,
            String action,
            Resource resource,
            Map<String, Object> environment
    ) {
    }

    public record Subject(
            java.util.UUID userId,
            String tenantId,
            Map<String, Object> attributes
    ) {
    }

    public record Resource(
            String type,
            String id,
            Map<String, Object> attributes
    ) {
    }

    /**
     * Wire shape matches the admin UI's {@code PolicyTestResponse} interface.
     *
     * <p>{@code reason} and {@code matchedRules} carry whatever the underlying
     * policy chose to expose via {@link PolicyEvaluation#reason()} /
     * {@link PolicyEvaluation#matchedRules()}; policies that only implement
     * the coarse {@code evaluate} entry point land here with {@code reason}
     * = null and an empty {@code matchedRules}.
     */
    public record PolicyTestResponse(
            PolicyDecision effect,
            String reason,
            List<String> matchedRules
    ) {
    }
}
