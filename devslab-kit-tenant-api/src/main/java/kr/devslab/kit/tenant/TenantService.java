package kr.devslab.kit.tenant;

import java.util.List;
import java.util.Optional;
import kr.devslab.kit.core.id.TenantId;

public interface TenantService {

    TenantMetadata create(TenantId id, String name, TenantMode mode);

    void rename(TenantId id, String newName);

    void deactivate(TenantId id);

    void activate(TenantId id);

    void delete(TenantId id);

    Optional<TenantMetadata> findById(TenantId id);

    List<TenantMetadata> findAll();
}
