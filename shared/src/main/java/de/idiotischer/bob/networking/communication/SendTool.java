package de.idiotischer.bob.networking.communication;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;

public record SendTool(SharedCore sharedCore) {

    public void send(AsynchronousSocketChannel channel, Packet packet) {
        if (channel == null || !channel.isOpen()) return;

        ByteBuffer buffer = sharedCore.getRegistry().getEncoder().code(packet);

        if(buffer == null) return;

        channel.write(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
                try {
                    channel.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void broadcast(Set<AsynchronousSocketChannel> clients, Packet packet) {
        clients.forEach(client -> send(client, packet));
    }
}