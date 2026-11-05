package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.FocusTree;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FocusTreesSyncPacket implements Packet {

    private List<FocusTreeSyncPacket> packets = new ArrayList<>();

    public FocusTreesSyncPacket() {}

    public FocusTreesSyncPacket(List<FocusTreeSyncPacket> packets) {
        this.packets = packets;
    }

    public static FocusTreesSyncPacket fromFocuses(Set<FocusTree> focuses) {
        return new FocusTreesSyncPacket(focuses.stream().map(FocusTreeSyncPacket::new).toList());
    }

    public static FocusTreesSyncPacket fromFocuses(List<FocusTree> focuses) {
        return new FocusTreesSyncPacket(focuses.stream().map(FocusTreeSyncPacket::new).toList());
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
            FocusTreeSyncPacket packet = new FocusTreeSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<FocusTreeSyncPacket> getPackets() {
        return packets;
    }
}
