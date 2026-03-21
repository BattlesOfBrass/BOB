package de.idiotischer.bob.networking.packet.codec;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.networking.packet.PacketRegistry;

import java.nio.ByteBuffer;

public class PacketEncoder implements Coder<ByteBuffer, Packet> {

    private final PacketRegistry registry;

    public PacketEncoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ByteBuffer code(Packet packet) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.putInt(registry.getPacketId(packet.getClass()));
        packet.write(buffer);

        PacketRegistry.PacketSendEvent event = new PacketRegistry.PacketSendEvent(packet);

        registry.getCore().getListenerRegistry().call(event);

        if(event.isCancelled()) {
            return null;
        }

        //APIInstance.getEventManager().call(new PacketRegistry.PacketSendEvent(packet, channelHandlerContext));
        return buffer;
    }
}
