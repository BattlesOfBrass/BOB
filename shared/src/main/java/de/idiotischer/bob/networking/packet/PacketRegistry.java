package de.idiotischer.bob.networking.packet;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.codec.PacketDecoder;
import de.idiotischer.bob.networking.packet.codec.PacketEncoder;
import de.idiotischer.bob.networking.packet.impl.PingPacket;

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

    public static class PacketReceiveEvent extends CancellableEvent {
        private final Packet packet;

        public PacketReceiveEvent(Packet packet) {
            this.packet = packet;

        }

        public Packet getPacket() {
            return packet;
        }

    }

    public static class PacketSendEvent extends CancellableEvent {
        private final Packet packet;

        public PacketSendEvent(Packet packet) {
            this.packet = packet;
        }

        public Packet getPacket() {
            return packet;
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
