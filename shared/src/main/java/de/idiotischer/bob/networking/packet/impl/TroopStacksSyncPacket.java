package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.troop.TroopStack;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class TroopStacksSyncPacket implements Packet {

    private List<TroopStackSyncPacket> packets = new ArrayList<>();

    public TroopStacksSyncPacket() {}

    public TroopStacksSyncPacket(List<TroopStackSyncPacket> packets) {
        this.packets = packets;
    }

    public static TroopStacksSyncPacket fromStates(Map<UUID,TroopStack> states) {
        return new TroopStacksSyncPacket(states.entrySet().stream().map(t -> new TroopStackSyncPacket(t.getKey(), t.getValue())).toList());
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
            TroopStackSyncPacket packet = new TroopStackSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<TroopStackSyncPacket> getPackets() {
        return packets;
    }
}
