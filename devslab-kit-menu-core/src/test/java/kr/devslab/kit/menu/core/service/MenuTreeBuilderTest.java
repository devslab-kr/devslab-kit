package kr.devslab.kit.menu.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.devslab.kit.menu.core.entity.PlatformMenuEntity;
import org.junit.jupiter.api.Test;

class MenuTreeBuilderTest {

    private final MenuTreeBuilder builder = new MenuTreeBuilder();

    @Test
    void buildsFlatRootsSortedBySortOrder() {
        var b = entity("B", null, 2);
        var a = entity("A", null, 1);
        var c = entity("C", null, 3);

        var tree = builder.build(List.of(b, a, c));

        assertThat(tree.roots()).extracting("code").containsExactly("A", "B", "C");
    }

    @Test
    void buildsParentChildTreeAndSortsChildren() {
        var parent = entity("parent", null, 1);
        var child2 = entity("child2", parent.getId(), 2);
        var child1 = entity("child1", parent.getId(), 1);

        var tree = builder.build(List.of(parent, child2, child1));

        assertThat(tree.roots()).hasSize(1);
        var root = tree.roots().get(0);
        assertThat(root.code()).isEqualTo("parent");
        assertThat(root.children()).extracting("code").containsExactly("child1", "child2");
    }

    @Test
    void mapsRequiredPermissionCodeToOptionalPermission() {
        var locked = entity("locked", null, 1);
        locked.setRequiredPermissionCode("admin.read");
        var open = entity("open", null, 2);

        var tree = builder.build(List.of(locked, open));

        assertThat(tree.roots().get(0).requiredPermission()).isPresent();
        assertThat(tree.roots().get(0).requiredPermission().get().code()).isEqualTo("admin.read");
        assertThat(tree.roots().get(1).requiredPermission()).isEmpty();
    }

    private PlatformMenuEntity entity(String code, UUID parentId, int sortOrder) {
        return new PlatformMenuEntity(
                UUID.randomUUID(),
                "default",
                code,
                code + " label",
                "/" + code,
                parentId,
                sortOrder,
                null,
                null,
                Instant.now()
        );
    }
}
