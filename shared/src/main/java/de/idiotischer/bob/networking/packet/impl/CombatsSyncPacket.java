package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.combat.CombatStatus;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.troop.TroopResolver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CombatsSyncPacket implements Packet {

    private List<CombatSyncPacket> packets = new ArrayList<>();

    public CombatsSyncPacket() {}

    public CombatsSyncPacket(List<CombatSyncPacket> packets) {
        this.packets = packets;
    }

    public CombatsSyncPacket fromCombats(List<CombatStatus> statuses, TroopResolver resolver) {
        return new CombatsSyncPacket(statuses.stream().map(c -> new CombatSyncPacket(c, resolver)).toList());
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
            CombatSyncPacket packet = new CombatSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }

    }

    public List<CombatSyncPacket> getPackets() {
        return packets;
    }
}
