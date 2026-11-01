package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;

import java.util.Set;

public interface FocusResolver {
    FocusTree getFocusTree(Country c);

    Set<Country> getWithFocusTree(FocusTree f);

    Focus focusBy(String treeId, String id);

    FocusTree treeBy(String id);

    Set<Focus> getFor(FocusTree tree);

    Focus getActiveFocus(Country c);
}
