package de.idiotischer.bob.game;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.nio.channels.AsynchronousSocketChannel;

public class ServerGameManager {

   public GameState state = GameState.INGAME;

    public GameState getState() {
        return state;
    }

    public void sendTile(AsynchronousSocketChannel sock) {
        Server.getInstance().getSendTool().send(sock, new ReplyPacket(Type.GAMESTATE_SYNC, String.valueOf(state.ordinal())));
    }

    public void broadcastTile(AsynchronousSocketChannel sock) {
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.GAMESTATE_SYNC, String.valueOf(state.ordinal())));
    }

    public void tickTime() {
        if(state == GameState.PAUSED || state == GameState.WAITING) return;
    }
}
