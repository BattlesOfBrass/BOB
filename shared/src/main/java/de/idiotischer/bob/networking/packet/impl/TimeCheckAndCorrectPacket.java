package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;

public class TimeCheckAndCorrectPacket implements Packet {
    public TimeCheckAndCorrectPacket() {}

    public TimeCheckAndCorrectPacket(long timeTicks, int speed) {
    }

    @Override
    public void write(ByteBuffer buffer) {

    }

    @Override
    public void read(ByteBuffer buffer) {

    }
}
