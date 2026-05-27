package kr.devslab.kit.access.policy;

public interface Policy {

    String name();

    /**
     * Human-readable explanation of what this policy does, surfaced in the
     * admin UI's policy list. Implementations can leave this null to fall
     * back to the policy name.
     */
    default String description() {
        return null;
    }

    /**
     * Coarse-grained evaluation entry point — returns just the
     * {@link PolicyDecision} enum.
     *
     * <p>Every policy must implement this method. Policies that want to
     * surface a reason or the rules they matched should ALSO override
     * {@link #evaluateDetailed(PolicyContext)}; the default implementation
     * of that method wraps the result of {@code evaluate} with an empty
     * reason / matched-rule list.
     */
    PolicyDecision evaluate(PolicyContext context);

    /**
     * Detailed evaluation entry point — returns the decision wrapped with
     * an optional reason + matched-rule list.
     *
     * <p>Default implementation delegates to {@link #evaluate(PolicyContext)}
     * so legacy policies that only override {@code evaluate} keep producing
     * a valid {@link PolicyEvaluation} (with null reason + empty matched
     * rules) for the admin UI's policy tester. Override this method
     * directly when you have detail worth surfacing.
     */
    default PolicyEvaluation evaluateDetailed(PolicyContext context) {
        return PolicyEvaluation.of(evaluate(context));
    }
}
