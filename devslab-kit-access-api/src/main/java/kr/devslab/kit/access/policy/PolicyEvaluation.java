package kr.devslab.kit.access.policy;

import java.util.List;
import java.util.Objects;

/**
 * Richer outcome of a {@link Policy} evaluation.
 *
 * <p>Wraps the {@link PolicyDecision} enum with two debugging signals the
 * admin UI surfaces in its policy tester:
 *
 * <ul>
 *   <li>{@code reason} — short human-readable explanation of why the
 *       policy chose what it chose ({@code null} if the policy didn't
 *       supply one).</li>
 *   <li>{@code matchedRules} — the named rules / conditions inside the
 *       policy that fired against this {@link PolicyContext}. Empty when
 *       the policy is opaque or when the decision is
 *       {@link PolicyDecision#NOT_APPLICABLE}.</li>
 * </ul>
 *
 * <p>Constructed via the factory methods so call-sites stay readable
 * and the {@code matchedRules} list is always non-null + immutable.
 */
public record PolicyEvaluation(
        PolicyDecision decision,
        String reason,
        List<String> matchedRules
) {

    public PolicyEvaluation {
        Objects.requireNonNull(decision, "PolicyEvaluation decision must not be null");
        matchedRules = matchedRules == null ? List.of() : List.copyOf(matchedRules);
    }

    public static PolicyEvaluation permit(String reason, List<String> matchedRules) {
        return new PolicyEvaluation(PolicyDecision.PERMIT, reason, matchedRules);
    }

    public static PolicyEvaluation deny(String reason, List<String> matchedRules) {
        return new PolicyEvaluation(PolicyDecision.DENY, reason, matchedRules);
    }

    public static PolicyEvaluation notApplicable(String reason) {
        return new PolicyEvaluation(PolicyDecision.NOT_APPLICABLE, reason, List.of());
    }

    /**
     * Convenience for policies that only have a coarse-grained decision
     * to share. Use the named factory methods when you can — they make
     * the intent obvious at the call-site.
     */
    public static PolicyEvaluation of(PolicyDecision decision) {
        return new PolicyEvaluation(decision, null, List.of());
    }
}
