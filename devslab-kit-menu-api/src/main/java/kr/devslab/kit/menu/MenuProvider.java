package kr.devslab.kit.menu;

import kr.devslab.kit.identity.CurrentUser;

public interface MenuProvider {

    MenuTree menusFor(CurrentUser user);
}
