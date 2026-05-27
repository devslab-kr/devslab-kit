package kr.devslab.kit.access.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kr.devslab.kit.access.policy.Policy;
import kr.devslab.kit.access.policy.PolicyContext;
import kr.devslab.kit.access.policy.PolicyDecision;
import org.junit.jupiter.api.Test;

class DefaultPolicyEvaluatorTest {

    private final Policy alwaysAllow = new Policy() {
        public String name() { return "always-allow"; }
        public PolicyDecision evaluate(PolicyContext ctx) { return PolicyDecision.PERMIT; }
    };

    private final Policy alwaysDeny = new Policy() {
        public String name() { return "always-deny"; }
        public PolicyDecision evaluate(PolicyContext ctx) { return PolicyDecision.DENY; }
    };

    @Test
    void returnsPermitFromMatchingPolicy() {
        var evaluator = new DefaultPolicyEvaluator(List.of(alwaysAllow));

        assertThat(evaluator.evaluate("always-allow", PolicyContext.builder().build()))
                .isEqualTo(PolicyDecision.PERMIT);
    }

    @Test
    void returnsDenyFromMatchingPolicy() {
        var evaluator = new DefaultPolicyEvaluator(List.of(alwaysDeny));

        assertThat(evaluator.evaluate("always-deny", PolicyContext.builder().build()))
                .isEqualTo(PolicyDecision.DENY);
    }

    @Test
    void returnsNotApplicableForMissingPolicy() {
        var evaluator = new DefaultPolicyEvaluator(List.of(alwaysAllow));

        assertThat(evaluator.evaluate("unknown", PolicyContext.builder().build()))
                .isEqualTo(PolicyDecision.NOT_APPLICABLE);
        assertThat(evaluator.evaluate(null, PolicyContext.builder().build()))
                .isEqualTo(PolicyDecision.NOT_APPLICABLE);
    }

    @Test
    void throwsOnDuplicatePolicyNames() {
        Policy dup = new Policy() {
            public String name() { return "always-allow"; }
            public PolicyDecision evaluate(PolicyContext ctx) { return PolicyDecision.DENY; }
        };

        assertThatThrownBy(() -> new DefaultPolicyEvaluator(List.of(alwaysAllow, dup)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("always-allow");
    }
}
