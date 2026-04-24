package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.state.State;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StatesSyncPacket implements Packet {

    private List<StateSyncPacket> packets = new ArrayList<>();

    public StatesSyncPacket() {}

    public StatesSyncPacket(List<StateSyncPacket> packets) {
        this.packets = packets;
    }

    public static StatesSyncPacket fromStates(Set<State> states) {
        return new StatesSyncPacket(states.stream().map(StateSyncPacket::new).toList());
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
            StateSyncPacket packet = new StateSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<StateSyncPacket> getPackets() {
        return packets;
    }
}
