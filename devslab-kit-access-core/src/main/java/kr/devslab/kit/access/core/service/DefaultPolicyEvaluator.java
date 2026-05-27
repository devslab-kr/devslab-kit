package kr.devslab.kit.access.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import kr.devslab.kit.access.policy.PolicyEvaluation;
import kr.devslab.kit.access.policy.PolicyEvaluator;

public class DefaultPolicyEvaluator implements PolicyEvaluator {

    private final Map<String, Policy> policiesByName;

    public DefaultPolicyEvaluator(List<Policy> policies) {
        Map<String, Policy> map = new HashMap<>();
        for (Policy policy : policies) {
            Policy previous = map.put(policy.name(), policy);
            if (previous != null) {
                throw new IllegalStateException("Duplicate Policy bean for name: " + policy.name());
            }
        }
        this.policiesByName = Map.copyOf(map);
    }

    @Override
    public PolicyDecision evaluate(String policyName, PolicyContext context) {
        if (policyName == null) {
            return PolicyDecision.NOT_APPLICABLE;
        }
        Policy policy = policiesByName.get(policyName);
        if (policy == null) {
            return PolicyDecision.NOT_APPLICABLE;
        }
        return policy.evaluate(context);
    }

    @Override
    public PolicyEvaluation evaluateDetailed(String policyName, PolicyContext context) {
        if (policyName == null) {
            return PolicyEvaluation.notApplicable("policyName was null");
        }
        Policy policy = policiesByName.get(policyName);
        if (policy == null) {
            return PolicyEvaluation.notApplicable("no policy registered for name: " + policyName);
        }
        return policy.evaluateDetailed(context);
    }

    public Set<String> registeredNames() {
        return policiesByName.keySet();
    }

    /**
     * Returns the registered policies (used by {@code PolicyAdminController}
     * to surface name + description in the admin UI's policy list).
     */
    public java.util.Collection<Policy> registeredPolicies() {
        return policiesByName.values();
    }
}
