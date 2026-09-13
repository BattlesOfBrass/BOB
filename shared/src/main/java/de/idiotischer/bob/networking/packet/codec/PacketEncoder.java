package de.idiotischer.bob.networking.packet.codec;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.codec.Coder;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class PacketEncoder implements Coder<ByteBuffer, Packet> {

    private final PacketRegistry registry;

    public PacketEncoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ByteBuffer code(Packet packet, AsynchronousSocketChannel channel) {
        PacketRegistry.PacketSendEvent event = new PacketRegistry.PacketSendEvent(packet, channel);
        registry.getCore().getListenerRegistry().call(event);

        if (event.isCancelled()) {
            return null;
        }

        byte[] payload = getPacketBytes(packet);

        long totalSizeLong = 8L + payload.length;

        if (totalSizeLong > Integer.MAX_VALUE) {
            throw new RuntimeException("Packet size exceeds the 2GB Java ByteBuffer limit!");
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) totalSizeLong);
        buffer.putInt(4 + payload.length);
        buffer.putInt(registry.getPacketId(packet.getClass()));
        buffer.put(payload);

        buffer.flip();
        return buffer;
    }

    private byte[] getPacketBytes(Packet packet) {
        int currentSize = 256;

        while (true) {
            try {
                ByteBuffer temp = ByteBuffer.allocate(currentSize);

                packet.write(temp);

                temp.flip();
                byte[] bytes = new byte[temp.remaining()];
                temp.get(bytes);
                return bytes;

            } catch (java.nio.BufferOverflowException e) {
                if (currentSize > Integer.MAX_VALUE / 2) {
                    if (currentSize == Integer.MAX_VALUE) {
                        throw new RuntimeException("Packet is lwk too large for memory.");
                    }
                    currentSize = Integer.MAX_VALUE;
                } else {
                    currentSize *= 2;
                }
            }
        }
    }
}