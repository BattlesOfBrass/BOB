package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;

import java.util.Set;

public class ServerFocusManager implements FocusResolver {

    public void reload() {

    }

    @Override
    public FocusTree getFocus(Country c) {
        return null;
    }

    @Override
    public Set<Country> getWithFocusTree(FocusTree f) {
        return Set.of();
    }

    @Override
    public Focus focusBy(String id) {
        return null;
    }

    @Override
    public FocusTree treeBy(String id) {
        return null;
    }

    @Override
    public Set<Focus> getFor(FocusTree tree) {
        return Set.of();
    }
}
