package de.idiotischer.bob.event;

import de.craftsblock.craftscore.event.CancellableEvent;

import java.nio.channels.AsynchronousSocketChannel;

public class ClientConnectEvent extends CancellableEvent {
    private final AsynchronousSocketChannel channel;

    public ClientConnectEvent(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }
}
