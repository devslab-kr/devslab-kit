package kr.devslab.kit.menu;

import java.util.List;

public record MenuTree(List<MenuItem> roots) {

    public MenuTree {
        roots = roots == null ? List.of() : List.copyOf(roots);
    }
}
