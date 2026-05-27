package kr.devslab.kit.tenant;

/**
 * Lifecycle state of a {@link TenantMetadata}.
 *
 * <p>Three states:
 *
 * <ul>
 *   <li>{@link #ACTIVE} — normal operation. Authentication, tenant-scoped
 *       data access, and admin operations all succeed.</li>
 *   <li>{@link #SUSPENDED} — temporarily disabled. Existing users can't log
 *       in but data and config are preserved; an admin can promote back to
 *       {@code ACTIVE} at any time.</li>
 *   <li>{@link #ARCHIVED} — soft-deleted. Stays in the system for audit
 *       and reference but is considered terminal.</li>
 * </ul>
 *
 * <p>The vocabulary lines up with the admin UI's tenant page so the wire
 * shape and the UI tag set match without translation.
 */
public enum TenantStatus {
    ACTIVE,
    SUSPENDED,
    ARCHIVED
}
