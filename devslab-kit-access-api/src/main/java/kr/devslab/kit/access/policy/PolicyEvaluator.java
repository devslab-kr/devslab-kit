package kr.devslab.kit.access.policy;

public interface PolicyEvaluator {

    PolicyDecision evaluate(String policyName, PolicyContext context);

    /**
     * Detailed evaluation entry point — returns the decision wrapped with
     * an optional reason + matched-rule list, as supplied by the underlying
     * {@link Policy#evaluateDetailed(PolicyContext)}.
     *
     * <p>Default delegates to {@link #evaluate(String, PolicyContext)} so
     * legacy evaluators keep working unchanged. Override directly when the
     * evaluator can produce a richer answer than just wrapping the enum.
     */
    default PolicyEvaluation evaluateDetailed(String policyName, PolicyContext context) {
        return PolicyEvaluation.of(evaluate(policyName, context));
    }
}
