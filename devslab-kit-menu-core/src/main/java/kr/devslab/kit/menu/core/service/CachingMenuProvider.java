package kr.devslab.kit.menu.core.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.MenuTree;

/**
 * {@link MenuProvider} decorator that caches the per-user menu tree for a
 * configurable TTL.
 *
 * <p>Caching keyed by {@code CurrentUser.id()} — the menu tree the delegate
 * returns is already permission-filtered, so the result varies per user
 * even within the same tenant. Per-user keys keep the cache correct without
 * having to invalidate on tenant-wide menu edits (the TTL handles that).
 *
 * <p>Implementation deliberately small: a {@link ConcurrentHashMap} with a
 * per-entry expiry timestamp, no background eviction thread, no LRU bound.
 * Entries expire lazily on read; stale entries linger until a read evicts
 * them. For the usage we have today (a few dozen admins per tenant, menus
 * loaded once per session) this is plenty. Swap in Caffeine or a Spring
 * Cache if the access pattern outgrows it.
 *
 * <p>Manual invalidation is exposed for admin tooling that mutates menus
 * and wants the next read to skip the cache.
 */
public class CachingMenuProvider implements MenuProvider {

    private final MenuProvider delegate;
    private final Duration ttl;
    private final Clock clock;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachingMenuProvider(MenuProvider delegate, Duration ttl, Clock clock) {
        this.delegate = delegate;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public MenuTree menusFor(CurrentUser user) {
        UUID key = user.id().value();
        Instant now = Instant.now(clock);
        CacheEntry hit = cache.get(key);
        if (hit != null && hit.expiresAt().isAfter(now)) {
            return hit.tree();
        }
        // Recompute via the delegate. ConcurrentHashMap.put has racing-thunderr
        // semantics — two callers may both miss and both invoke the delegate
        // briefly; both writes are idempotent. Worth it to avoid holding a
        // lock across the DB round-trip.
        MenuTree fresh = delegate.menusFor(user);
        cache.put(key, new CacheEntry(fresh, now.plus(ttl)));
        return fresh;
    }

    public void invalidate(UUID userId) {
        cache.remove(userId);
    }

    public void invalidateAll() {
        cache.clear();
    }

    private record CacheEntry(MenuTree tree, Instant expiresAt) {
    }
}
