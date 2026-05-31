package kr.devslab.kit.admin;

public final class AdminApiPaths {

    public static final String BASE = "/admin/api/v1";

    public static final String USERS = BASE + "/users";
    public static final String ROLES = BASE + "/roles";
    public static final String PERMISSIONS = BASE + "/permissions";
    public static final String GROUPS = BASE + "/groups";
    public static final String MENUS = BASE + "/menus";
    public static final String AUDIT_LOGS = BASE + "/audit-logs";

    /** Unauthenticated: a setup wizard reads this before any account exists. */
    public static final String BOOTSTRAP_STATUS = BASE + "/bootstrap/status";

    private AdminApiPaths() {
    }
}
