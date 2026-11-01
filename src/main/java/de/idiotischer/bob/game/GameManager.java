package de.idiotischer.bob.game;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.time.TimeResolver;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class GameManager {

    private CompletableFuture<GameState> future = null;
    private GameState gameState = GameState.PAUSED;

    public CompletableFuture<GameState> getGameStateSync() {
        future = new CompletableFuture<>();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.GAMESTATE_SYNC));

        return future;
    }

    public void completeGameState(GameState gameState) {
        if(future != null && !future.isDone()) future.complete(gameState);

        this.gameState = gameState;
    }

    public CompletableFuture<GameState> getFuture() {
        return future;
    }

    public void sendTogglePause() {

    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

}
