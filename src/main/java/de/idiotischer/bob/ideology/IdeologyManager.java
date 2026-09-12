package de.idiotischer.bob.ideology;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class IdeologyManager {

    private final Set<Ideology> ideologies = new HashSet<>();
    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        ideologies.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.IDEOLOGIES_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload() {
        BOB.getInstance().getCountryManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public Set<Ideology> getIdeologies() {
        return ideologies;
    }

    public CompletableFuture<Void> getAwaitingFuture() {
        return awaitingFuture;
    }
}
