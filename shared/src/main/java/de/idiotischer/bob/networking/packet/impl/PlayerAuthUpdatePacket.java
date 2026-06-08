package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerAuthUpdatePacket implements Packet {
    private UUID uuid;
    private boolean authed;

    public PlayerAuthUpdatePacket() {}

    public PlayerAuthUpdatePacket(UUID uuid, boolean authed) {
        this.authed = authed;
        this.uuid = uuid;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        util.putUuid(uuid);
        util.putBoolean(authed);
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        uuid = util.getUuid();
        authed = util.getBoolean();
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAuthed() {
        return authed;
    }
}
