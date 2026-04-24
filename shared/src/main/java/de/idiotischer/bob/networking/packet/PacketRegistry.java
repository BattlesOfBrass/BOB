package de.idiotischer.bob.networking.packet;

import de.craftsblock.cnet.modules.packets.common.packet.BufferWritable;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftscore.event.CancellableEvent;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.codec.PacketDecoder;
import de.idiotischer.bob.networking.packet.codec.PacketEncoder;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import org.jetbrains.annotations.NotNull;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {
    private final PacketEncoder encoder;
    private final PacketDecoder decoder;
    private final SharedCore core;

    private final Map<Integer, Class<? extends Packet>> packetIds = new HashMap<>();

    public PacketRegistry(SharedCore core) {
        this.core = core;
        this.encoder = new PacketEncoder(this);
        this.decoder = new PacketDecoder(this);

        registerPacket(PingPacket.class, 0);
        registerPacket(PongPacket.class, 1);
        registerPacket(RequestPacket.class, 2);
        registerPacket(ScenarioSyncPacket.class, 3);
        registerPacket(ScenariosSyncPacket.class, 4);
        registerPacket(CountrySyncPacket.class, 5);
        registerPacket(CountriesSyncPacket.class, 6);
        registerPacket(StateSyncPacket.class, 7);
        registerPacket(StatesSyncPacket.class, 8);
    }

    public void registerPacket(Class<? extends Packet> packet, int id) {
        if(id < 0) {
            throw new IllegalArgumentException("ID cannot be negative please reregister the packet: " + packet.getSimpleName());
        }
        packetIds.put(id, packet);
    }

    public int getPacketId(Class<? extends Packet> packetClass) {
        for (Map.Entry<Integer, Class<? extends Packet>> entry : packetIds.entrySet()) {
            if (entry.getValue().equals(packetClass)) {
                return entry.getKey();
            }
        }

        return -1;
    }

    public boolean isIdValid(int id) {
        if(packetIds.containsKey(id)) return true;
        else if(id < 0) {
            return  false;
        }
        else {
            return false;
        }
    }

    public Class<? extends Packet> getPacketById(int id) {
        for (Map.Entry<Integer, Class<? extends Packet>> entry : packetIds.entrySet()) {
            if (entry.getKey() == id) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static class PacketReceiveEvent extends CancellableEvent implements BufferWritable {
        private final Packet packet;
        private final AsynchronousSocketChannel channel;

        public PacketReceiveEvent(Packet packet, AsynchronousSocketChannel channel) {
            this.packet = packet;
            this.channel = channel;
        }

        public Packet getPacket() {
            return packet;
        }

        public AsynchronousSocketChannel getChannel() {
            return channel;
        }

        @Override
        public void write(@NotNull BufferUtil buffer) {

        }
    }

    public static class PacketSendEvent extends CancellableEvent implements BufferWritable {
        private final Packet packet;
        private final AsynchronousSocketChannel channel;

        public PacketSendEvent(Packet packet, AsynchronousSocketChannel channel) {
            this.packet = packet;
            this.channel = channel;
        }

        public Packet getPacket() {
            return packet;
        }

        public AsynchronousSocketChannel getChannel() {
            return channel;
        }

        @Override
        public void write(@NotNull BufferUtil buffer) {

        }
    }


    public PacketEncoder getEncoder() {
        return encoder;
    }

    public PacketDecoder getDecoder() {
        return decoder;
    }

    public SharedCore getCore() { return core; }
}
