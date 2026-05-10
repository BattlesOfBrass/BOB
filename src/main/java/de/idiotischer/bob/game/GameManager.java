package de.idiotischer.bob.game;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.concurrent.CompletableFuture;

public class GameManager {

    private CompletableFuture<GameState> future = null;

    public CompletableFuture<GameState> getGameState() {
        future = new CompletableFuture<>();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.GAMESTATE_SYNC));

        return future;
    }

    public void completeGameState(GameState gameState) {
        if(future != null && !future.isDone()) future.complete(gameState);
    }

    public CompletableFuture<GameState> getFuture() {
        return future;
    }
}
