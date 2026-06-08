package de.idiotischer.bob.networking.communication;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public record SendTool(SharedCore sharedCore) {

    private static final Map<AsynchronousSocketChannel, Queue<ByteBuffer>> queues = new ConcurrentHashMap<>();
    private static final Map<AsynchronousSocketChannel, AtomicBoolean> writing = new ConcurrentHashMap<>();

    public void send(AsynchronousSocketChannel channel, Packet packet) {
        if (channel == null || !channel.isOpen()) return;

        ByteBuffer buffer = sharedCore.getRegistry().getEncoder().code(packet, channel);
        if (buffer == null) return;

        Queue<ByteBuffer> queue = queues.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>());
        AtomicBoolean isWriting = writing.computeIfAbsent(channel, k -> new AtomicBoolean(false));

        queue.add(buffer);
        writeNext(channel, queue, isWriting);
    }

    private void writeNext(AsynchronousSocketChannel channel, Queue<ByteBuffer> queue, AtomicBoolean isWriting) {
        if (isWriting.compareAndSet(false, true)) {
            ByteBuffer nextBuffer = queue.poll();
            if (nextBuffer == null) {
                isWriting.set(false);
                return;
            }

            channel.write(nextBuffer, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer result, Void attachment) {
                    isWriting.set(false);
                    writeNext(channel, queue, isWriting);
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    isWriting.set(false);
                    queues.remove(channel);
                    writing.remove(channel);
                    try {
                        channel.close();
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    public void broadcast(Set<AsynchronousSocketChannel> clients, Packet packet) {
        clients.forEach(client -> send(client, packet));
    }
}