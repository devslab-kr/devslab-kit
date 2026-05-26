package kr.devslab.kit.menu.core.service;

import java.util.List;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.menu.MenuProvider;
import kr.devslab.kit.menu.MenuTree;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import kr.devslab.kit.menu.core.repository.JpaPlatformMenuRepository;
import org.springframework.transaction.annotation.Transactional;

public class DefaultMenuProvider implements MenuProvider {

    private final JpaPlatformMenuRepository repository;
    private final MenuTreeBuilder treeBuilder;
    private final PermissionBasedMenuFilter filter;

    public DefaultMenuProvider(
            JpaPlatformMenuRepository repository,
            MenuTreeBuilder treeBuilder,
            PermissionBasedMenuFilter filter
    ) {
        this.repository = repository;
        this.treeBuilder = treeBuilder;
        this.filter = filter;
    }

    @Override
    @Transactional(readOnly = true)
    public MenuTree menusFor(CurrentUser user) {
        List<PlatformMenuEntity> entities = repository.findAllByTenantIdOrderBySortOrderAsc(user.tenantId().value());
        MenuTree fullTree = treeBuilder.build(entities);
        return filter.filter(fullTree);
    }
}
