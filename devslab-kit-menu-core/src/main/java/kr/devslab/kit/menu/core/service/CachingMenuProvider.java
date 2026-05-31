package kr.devslab.kit.menu.core.service;

import kr.devslab.kit.cache.CacheNames;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.MenuTree;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * {@link MenuProvider} decorator that caches the per-user menu tree on the
 * kit's shared {@link CacheManager} (ADR 0002 §5).
 *
 * <p>Previously this kept its own {@code ConcurrentHashMap}, which went stale
 * across replicas. Riding the shared cache manager means the menu cache inherits
 * whatever backend the consumer chose with {@code devslab.kit.cache.type}: a
 * {@code ConcurrentMapCacheManager} for {@code in-memory} (single node, same as
 * before), a real distributed cache for {@code redis} (correct across replicas,
 * with the kit's JSON serialization), or a {@code NoOpCacheManager} for
 * {@code none} (every read hits the database — the right behaviour for tests
 * that mutate menus and expect to see the change immediately, replacing the old
 * "zero TTL disables the decorator" special case).
 *
 * <p>Keyed by {@code CurrentUser.id()} — the delegate's tree is already
 * permission-filtered, so it varies per user even within a tenant. TTL is owned
 * by the cache backend ({@code devslab.kit.cache.ttl} for Redis), not this class.
 *
 * <p>Manual eviction is exposed for admin tooling that mutates menus and wants
 * the next read recomputed.
 */
public class CachingMenuProvider implements MenuProvider {

    private final MenuProvider delegate;
    private final CacheManager cacheManager;

    public CachingMenuProvider(MenuProvider delegate, CacheManager cacheManager) {
        this.delegate = delegate;
        this.cacheManager = cacheManager;
    }

    @Override
    public MenuTree menusFor(CurrentUser user) {
        Cache cache = cacheManager.getCache(CacheNames.MENU);
        if (cache == null) {
            // Manager declined to provide the cache (e.g. a fixed-name manager
            // that doesn't know MENU) — fall back to the live delegate rather
            // than fail. Correctness over caching.
            return delegate.menusFor(user);
        }
        // get(key, valueLoader): atomic cache-or-compute. On a miss the loader
        // runs the DB-backed delegate and the result is stored under the user id.
        return cache.get(user.id().value(), () -> delegate.menusFor(user));
    }

    /** Evict one user's cached menu tree (e.g. after editing their visible menus). */
    public void invalidate(java.util.UUID userId) {
        Cache cache = cacheManager.getCache(CacheNames.MENU);
        if (cache != null) {
            cache.evict(userId);
        }
    }

    /** Evict every cached menu tree (e.g. after a tenant-wide menu change). */
    public void invalidateAll() {
        Cache cache = cacheManager.getCache(CacheNames.MENU);
        if (cache != null) {
            cache.clear();
        }
    }
}
