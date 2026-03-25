package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PingPacket implements Packet {
    @Override
    public void write(ByteBuffer buffer) {
        buffer.put("PING".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void read(ByteBuffer buffer) {
        System.out.println("PingPacket read");
    }
}
