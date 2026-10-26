package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.faction.FactionResolver;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FactionsSyncPacket implements Packet {

    private List<FactionSyncPacket> packets = new ArrayList<>();

    public FactionsSyncPacket() {}

    public FactionsSyncPacket(List<FactionSyncPacket> packets) {
        this.packets = packets;
    }

    public static FactionsSyncPacket fromFactions(Set<Faction> factions) {
        return new FactionsSyncPacket(factions.stream().map(FactionSyncPacket::new).toList());
    }

    public static FactionsSyncPacket fromFactions(List<Faction> factions) {
        return new FactionsSyncPacket(factions.stream().map(FactionSyncPacket::new).toList());
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(packets.size());

        packets.forEach(packet -> packet.write(buffer));
    }

    @Override
    public void read(ByteBuffer buffer) {
        packets.clear();

        int size = buffer.getInt();
        for (int i = 0; i < size; i++) {
            FactionSyncPacket packet = new FactionSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<FactionSyncPacket> getPackets() {
        return packets;
    }
}
