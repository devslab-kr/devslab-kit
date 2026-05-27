package kr.devslab.kit.admin.policy;

import java.util.List;
import java.util.Optional;
import kr.devslab.kit.access.core.service.DefaultPolicyEvaluator;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import kr.devslab.kit.access.policy.PolicyEvaluator;
import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AdminApiPaths.BASE + "/policies")
public class PolicyAdminController {

    private final PolicyEvaluator evaluator;

    public PolicyAdminController(PolicyEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @GetMapping
    public List<String> list() {
        if (evaluator instanceof DefaultPolicyEvaluator def) {
            return def.registeredNames().stream().sorted().toList();
        }
        return List.of();
    }

    @PostMapping("/{name}/test")
    public PolicyTestResponse test(@PathVariable String name, @RequestBody PolicyTestRequest req) {
        PolicyContext ctx = PolicyContext.builder()
                .user(req.userId() == null ? null : UserId.of(req.userId()))
                .tenant(req.tenantId() == null ? null : TenantId.of(req.tenantId()))
                .resource(req.resourceType(), req.resourceId())
                .resourceAttributes(Optional.ofNullable(req.resourceAttributes()).orElseGet(java.util.Map::of))
                .environmentAttributes(Optional.ofNullable(req.environmentAttributes()).orElseGet(java.util.Map::of))
                .build();
        PolicyDecision decision = evaluator.evaluate(name, ctx);
        return new PolicyTestResponse(name, decision);
    }

    public record PolicyTestResponse(String name, PolicyDecision decision) {
    }
}
