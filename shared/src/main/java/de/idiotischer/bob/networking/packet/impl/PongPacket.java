package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;

import java.nio.ByteBuffer;

public class PongPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {
    @Override
    public void write(BufferUtil buffer) {
    }

    @Override
    public void handle(Networker networker) {

    }

    @Override
    public void write(ByteBuffer buffer) {

    }

    @Override
    public void read(ByteBuffer buffer) {

    }
}
