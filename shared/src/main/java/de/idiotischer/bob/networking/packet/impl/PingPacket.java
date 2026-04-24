package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PingPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {

    @Override
    public void handle(Networker networker) {

    }

    @Override
    public void write(@NotNull BufferUtil buffer) {

    }

    @Override
    public void write(ByteBuffer buffer) {

    }

    @Override
    public void read(ByteBuffer buffer) {

    }
}
