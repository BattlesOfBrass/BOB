package de.idiotischer.bob.networking.packet.codec;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.networking.packet.PacketRegistry;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class PacketDecoder implements Coder<Packet, ByteBuffer> {

    private final PacketRegistry registry;

    public PacketDecoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Packet code(ByteBuffer byteBuf) {
        var id = byteBuf.getInt();

        if (!registry.isIdValid(id)) {
            throw new IllegalArgumentException("no packet with id " + id + " found!");
        }

        Packet packet;
        try {
            packet =  Objects.requireNonNull(registry.getPacketById(id)).getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        packet.read(byteBuf);

        PacketRegistry.PacketReceiveEvent event = new PacketRegistry.PacketReceiveEvent(packet);

        registry.getCore().getListenerRegistry().call(event);

        if(event.isCancelled()) {
            return null;
        }

        //APIInstance.getEventManager().call(new PacketRegistry.PacketReceiveEvent(packet));
        return packet;
    }
}
