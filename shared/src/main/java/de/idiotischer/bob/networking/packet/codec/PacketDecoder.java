package de.idiotischer.bob.networking.packet.codec;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.codec.Coder;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;

public class PacketDecoder implements Coder<Packet, ByteBuffer> {

    private final PacketRegistry registry;

    public PacketDecoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Packet code(ByteBuffer buffer, AsynchronousSocketChannel channel) {
        if (buffer.remaining() < 4) {
            return null;
        }

        buffer.mark();
        int length = buffer.getInt();

        if (buffer.remaining() < length) {
            buffer.reset();
            return null;
        }

        int id = buffer.getInt();

        if (!registry.isIdValid(id)) {
            throw new IllegalArgumentException("No packet with id " + id + " found!");
        }

        Packet packet;
        try {
            packet = Objects.requireNonNull(registry.getPacketById(id))
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create packet instance for id " + id, e);
        }

        int payloadLength = length - 4;
        int oldLimit = buffer.limit();

        buffer.limit(buffer.position() + payloadLength);

        try {
            packet.read(buffer);
        } finally {
            buffer.limit(oldLimit);
        }

        PacketRegistry.PacketReceiveEvent event =
                new PacketRegistry.PacketReceiveEvent(packet, channel);

        registry.getCore().getListenerRegistry().call(event);

        if (event.isCancelled()) {
            return packet;
        }

        return packet;
    }
}