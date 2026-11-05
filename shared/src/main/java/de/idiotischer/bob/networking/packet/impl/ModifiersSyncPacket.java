package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModifiersSyncPacket implements Packet {

    private List<ModifierSyncPacket> packets = new ArrayList<>();

    public ModifiersSyncPacket() {}

    public ModifiersSyncPacket(List<ModifierSyncPacket> packets) {
        this.packets = packets;
    }

    public static ModifiersSyncPacket fromModifiers(Set<Modifier> factions) {
        return new ModifiersSyncPacket(factions.stream().map(ModifierSyncPacket::new).toList());
    }

    public static ModifiersSyncPacket fromModifiers(List<Modifier> factions) {
        return new ModifiersSyncPacket(factions.stream().map(ModifierSyncPacket::new).toList());
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
            ModifierSyncPacket packet = new ModifierSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<ModifierSyncPacket> getPackets() {
        return packets;
    }
}
