package kr.devslab.kit.access.policy;

public interface Policy {

    String name();

    PolicyDecision evaluate(PolicyContext context);
}
