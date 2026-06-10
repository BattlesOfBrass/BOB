package de.idiotischer.bob.troop;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;
import de.idiotischer.bob.tile.Tile;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TroopManager {

    private final List<TroopStack> troops = new ArrayList<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    public TroopManager() {
        //reload();
    }


    public CompletableFuture<TroopStack> move(TroopStack selected, Tile newTile) {
        selected.setTile(newTile);

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        troops.clear();

        BOB.getInstance().getMainRenderer().getGamePanel().selected.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload(List<TroopStack> troops) {
        this.troops.addAll(troops);

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public void addTroopStack(TroopStack stack) {
        troops.add(stack);
    }

    public List<TroopStack> getEnemy() {
        return List.of();
    }

    public List<TroopStack> getVisible(Country country) {
        return troops;
    }

    public List<TroopStack> getFor(Country country) {
        return List.of();
    }

    public List<TroopStack> getAll() {
        return troops;
    }
}
