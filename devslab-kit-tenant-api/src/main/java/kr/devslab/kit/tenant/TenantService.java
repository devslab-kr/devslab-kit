package kr.devslab.kit.tenant;

import java.util.List;
import java.util.Optional;
import kr.devslab.kit.core.id.TenantId;

public interface TenantService {

    TenantMetadata create(TenantId id, String name, TenantMode mode);

    void rename(TenantId id, String newName);

    /**
     * Move a tenant to the given lifecycle state. The activate / deactivate
     * shorthands below are kept for callers that don't need the full enum.
     */
    void setStatus(TenantId id, TenantStatus status);

    /** Convenience for {@code setStatus(id, TenantStatus.SUSPENDED)}. */
    default void deactivate(TenantId id) {
        setStatus(id, TenantStatus.SUSPENDED);
    }

    /** Convenience for {@code setStatus(id, TenantStatus.ACTIVE)}. */
    default void activate(TenantId id) {
        setStatus(id, TenantStatus.ACTIVE);
    }

    void delete(TenantId id);

    Optional<TenantMetadata> findById(TenantId id);

    List<TenantMetadata> findAll();
}
