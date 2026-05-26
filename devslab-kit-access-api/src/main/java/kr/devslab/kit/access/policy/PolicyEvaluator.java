package kr.devslab.kit.access.policy;

public interface PolicyEvaluator {

    PolicyDecision evaluate(String policyName, PolicyContext context);
}
