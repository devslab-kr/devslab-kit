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

    PolicyDecision evaluate(PolicyContext context);
}
