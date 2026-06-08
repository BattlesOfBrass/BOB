package de.idiotischer.bob.networking.packet;

import java.nio.ByteBuffer;

public interface Packet {
    void write(ByteBuffer buffer);
    void read(ByteBuffer buffer);
}
