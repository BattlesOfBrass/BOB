package de.idiotischer.bob.focus;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FocusManager implements FocusResolver{

    private final Set<FocusTree> focusTrees = new HashSet<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        focusTrees.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.FOCUSES_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload() {
        BOB.getInstance().getFactionManager().reloadRules();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    @Override
    public FocusTree getFocusTree(Country c) {
        return null;
    }

    @Override
    public Set<Country> getWithFocusTree(FocusTree f) {
        return Set.of();
    }

    @Override
    public Focus focusBy(String treeId, String id) {
        return null;
    }

    @Override
    public FocusTree treeBy(String id) {
        return focusTrees.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Set<Focus> getFor(FocusTree tree) {
        return Set.of();
    }

    @Override
    public Focus getActiveFocus(Country c) {
        return null;
    }

    public void add(FocusTree tree) {
        this.focusTrees.add(tree);
    }

    public void remove(FocusTree tree) {
        this.focusTrees.remove(tree);
    }

    public void remove(String id) {
        this.remove(treeBy(id));
    }

    public void startFocus(String country, String id, String treeId) {
        focusBy(treeId,id).startFocus(country);
    }

    public void cancelFocus(String country, String id, String treeId) {
        focusBy(treeId,id).cancelFocus(country);
    }

    public void endFocus(String country, String id, String treeId) {
        focusBy(treeId,id).endFocus(country);
        //focus finished, blabla fisplay it as popup bla bla
    }
}
