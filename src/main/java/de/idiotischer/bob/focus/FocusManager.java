package de.idiotischer.bob.focus;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import it.unimi.dsi.fastutil.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FocusManager implements FocusResolver{

    private final Set<FocusTree> focusTrees = new HashSet<>();
    private Map<String, Pair<String,String>> activeFocuses = new HashMap<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        focusTrees.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.FOCUSES_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload() {
        BOB.getInstance().getFactionManager().reloadRules();

        focusTrees.forEach(tree -> System.out.println(tree.id()));

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    @Override
    public FocusTree getFocusTree(Country c) {
        //sorted may throw
        return focusTrees.stream().filter(tr -> tr.hasTree(c)).sorted().findFirst().orElse(null);
    }

    @Override
    public Set<Country> getWithFocusTree(FocusTree f) {
        return Server.getInstance().getCountryManager().getCountries().stream().filter(f::hasTree).collect(Collectors.toSet());
    }

    @Override
    public Focus focusBy(String focusTree, String id) {
        return treeBy(focusTree).focuses().stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public FocusTree treeBy(String id) {
        return focusTrees.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Set<Focus> getFor(FocusTree tree) {
        return tree.focuses();
    }

    @Override
    public Focus getActiveFocus(Country c) {
        var pair = activeFocuses.get(c.getAbbreviation());
        return focusBy(pair.key(),pair.value());
    }

    public Focus getActiveFocus(String c) {
        var pair = activeFocuses.get(c);
        return focusBy(pair.key(),pair.value());
    }

    public Map<String, Pair<String, String>> getActiveFocuses() {
        return activeFocuses;
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

        activeFocuses.put(country, Pair.of(id,treeId));
    }

    public void cancelFocus(String country, String id, String treeId) {
        focusBy(treeId,id).cancelFocus(country);

        activeFocuses.remove(country);
    }

    public void endFocus(String country, String id, String treeId) {
        focusBy(treeId,id).endFocus(country);

        activeFocuses.remove(country);
        //focus finished, blabla display it as popup bla bla
    }
}
