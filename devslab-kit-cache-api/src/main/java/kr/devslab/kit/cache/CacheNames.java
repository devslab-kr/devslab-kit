package kr.devslab.kit.cache;

/**
 * Stable cache-name constants for the kit's own caches.
 *
 * <p>Consumers generally use Spring's {@code @Cacheable("their-own-name")}
 * directly and never need this. It exists so the kit's internal caches (and any
 * cross-module references to them) share one source of truth instead of
 * scattering string literals — e.g. an admin tool that evicts the menu cache
 * references {@link #MENU} rather than re-typing {@code "devslab-kit-menu"}.
 */
public final class CacheNames {

    /** Per-user, permission-filtered menu tree. Keyed by user id. */
    public static final String MENU = "devslab-kit-menu";

    private CacheNames() {
    }
}
