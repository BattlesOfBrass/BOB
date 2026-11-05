package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IdeologiesSyncPacket implements Packet {

    private List<IdeologySyncPacket> packets = new ArrayList<>();

    public IdeologiesSyncPacket() {}

    public IdeologiesSyncPacket(List<IdeologySyncPacket> packets) {
        this.packets = packets;
    }

    public static IdeologiesSyncPacket fromIdeologies(Set<Ideology> ideologies) {
        return new IdeologiesSyncPacket(ideologies.stream().map(IdeologySyncPacket::new).toList());
    }

    public static IdeologiesSyncPacket fromIdeologies(List<Ideology> ideologies) {
        return new IdeologiesSyncPacket(ideologies.stream().map(IdeologySyncPacket::new).toList());
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
            IdeologySyncPacket packet = new IdeologySyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<IdeologySyncPacket> getPackets() {
        return packets;
    }
}
