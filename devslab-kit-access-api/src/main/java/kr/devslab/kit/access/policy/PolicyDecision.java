package kr.devslab.kit.access.policy;

/**
 * Outcome of evaluating a {@link Policy}.
 *
 * <p>Wire names follow the XACML 3.0 vocabulary (PERMIT / DENY / NOT_APPLICABLE)
 * so the admin UI's policy tester and downstream auditors see a familiar shape.
 */
public enum PolicyDecision {
    PERMIT,
    DENY,
    NOT_APPLICABLE
}
