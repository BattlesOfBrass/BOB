package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerAuthUpdatePacket implements Packet {
    private InetSocketAddress address;
    private UUID uuid;
    private boolean authed;

    public PlayerAuthUpdatePacket() {}

    public PlayerAuthUpdatePacket(InetSocketAddress address, UUID uuid, boolean authed) {
        this.address = address;
        this.authed = authed;
        this.uuid = uuid;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        util.putUuid(uuid);
        util.putBoolean(authed);

        util.putUtf(address.getHostString());
        buffer.putInt(address.getPort());
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        uuid = util.getUuid();
        authed = util.getBoolean();

        String host = util.getUtf();
        int port = buffer.getInt();

        address = new InetSocketAddress(host, port);
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAuthed() {
        return authed;
    }

    public InetSocketAddress getAddress() {
        return address;
    }
}
