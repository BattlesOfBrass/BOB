package de.idiotischer.bob.networking.packet.codec;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.codec.Coder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.zip.Deflater;

public class PacketEncoder implements Coder<ByteBuffer, Packet> {

    private final PacketRegistry registry;

    public PacketEncoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ByteBuffer code(Packet packet, AsynchronousSocketChannel channel) {
        PacketRegistry.PacketSendEvent event = new PacketRegistry.PacketSendEvent(packet, channel);
        registry.getCore().getListenerRegistry().call(event);

        if (event.isCancelled()) return null;

        byte[] payload = deflate(getPacketBytes(packet));

        long totalSizeLong = 8L + payload.length;

        if (totalSizeLong > Integer.MAX_VALUE) throw new RuntimeException("Packet size exceeds the 2GB Java ByteBuffer limit!");

        ByteBuffer buffer = ByteBuffer.allocate((int) totalSizeLong);
        buffer.putInt(4 + payload.length);
        buffer.putInt(registry.getPacketId(packet.getClass()));
        buffer.put(payload);

        buffer.flip();
        return buffer;
    }

    public byte[] deflate(byte[] data) {
        byte[] payload = data;
        boolean compressed = false;

        if(data.length >= SharedCore.getMinCompressionSize()) {
            Deflater defl = new Deflater();

            try {
                defl.setInput(data);
                defl.finish();

                byte[] part = new byte[4096];

                try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    while(!defl.finished()) {
                        int length = defl.deflate(part);

                        out.write(part,0,length);
                    }

                    byte[] res = out.toByteArray();

                    if(res.length + SharedCore.getMinSaving() < data.length) {
                        payload = res;
                        compressed = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                defl.end();
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(1 + payload.length);
        buf.put((byte) (compressed ? 1 : 0));
        buf.put(payload);

        return buf.array();
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