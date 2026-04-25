package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerJoinPacket implements Packet {

    private final InetSocketAddress address;
    private UUID uuid;

    public PlayerJoinPacket(UUID uuid, InetSocketAddress address) {
        this.uuid = uuid;
        this.address = address;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUuid(uuid);
        BufferUtil.of(buffer).putUtf(address.getHostName() + ";" + address.getPort());
    }

    @Override
    public void read(ByteBuffer buffer) {
        uuid = BufferUtil.of(buffer).getUuid();
    }

    public UUID getUuid() {
        return uuid;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
