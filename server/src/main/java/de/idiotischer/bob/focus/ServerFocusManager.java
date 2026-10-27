package de.idiotischer.bob.focus;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ServerFocusManager implements FocusResolver {

    public void reload() {
        Path globalFocuses = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getGlobalFocuses();
        Path focusTrees = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getFocusTree();

        if(Files.exists(globalFocuses)) loadFocuses(Set.of(globalFocuses));

        if(Files.exists(focusTrees)) {
            Set<Path> focusJsons = new HashSet<>();

            try {
                Files.walk(focusTrees).forEach(path -> {
                    if(!Files.isRegularFile(path)) return;

                    if(path.endsWith(".json")) focusJsons.add(path);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            loadFocuses(focusJsons);
            loadTrees(focusJsons);
        }
    }

    private void loadTrees(Set<Path> focusJsons) {

    }

    private void loadFocuses(Set<Path> focusJsons) {

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
