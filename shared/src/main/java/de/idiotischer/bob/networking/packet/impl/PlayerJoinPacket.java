package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerJoinPacket implements Packet {

    private boolean authorized;
    private InetSocketAddress address;
    private UUID uuid;

    public PlayerJoinPacket() {}

    public PlayerJoinPacket(boolean authorized, UUID uuid, InetSocketAddress address) {
        this.uuid = uuid;
        this.address = address;
        this.authorized = authorized;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil buf = BufferUtil.of(buffer);
        buf.putUuid(uuid);
        buf.putUtf(address.getHostName() + ";" + address.getPort());
        buf.putBoolean(authorized);
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil buf = BufferUtil.of(buffer);

        uuid = buf.getUuid();

        String[] s = buf.getUtf().split(";");

        address = new InetSocketAddress(s[0],Integer.parseInt(s[1]));

        authorized = buf.getBoolean();
    }

    public UUID getUuid() {
        return uuid;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}
