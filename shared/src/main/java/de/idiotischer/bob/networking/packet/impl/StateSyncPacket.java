package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StateSyncPacket implements Packet {
    private List<String> states;

    //map aus State.toString()
    public StateSyncPacket(List<String> stateAndOwners) {
        states = stateAndOwners;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(states.size());

        for (String s : states) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
    }

    @Override
    public void read(ByteBuffer buffer) {
        states = new ArrayList<>();

        int size = buffer.getInt();

        for (int i = 0; i < size; i++) {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            states.add(new String(bytes, StandardCharsets.UTF_8));
        }
    }
}
