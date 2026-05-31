package kr.devslab.kit.autoconfigure;

/**
 * The single root package under which all of devslab-kit's JPA {@code @Entity}
 * types (in {@code kr.devslab.kit.*.core.entity}) and Spring Data repositories
 * (in {@code kr.devslab.kit.*.core.repository}) live. Used by the persistence
 * auto-registration so a consumer never has to name the kit's packages.
 */
final class DevslabKitPackages {

    static final String ROOT = "kr.devslab.kit";

    private DevslabKitPackages() {
    }
}
