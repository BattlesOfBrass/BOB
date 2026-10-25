package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerQuitPacket implements Packet {

    private UUID uuid;

    public PlayerQuitPacket() {}

    public PlayerQuitPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUuid(uuid);
    }

    @Override
    public void read(ByteBuffer buffer) {
        uuid = BufferUtil.of(buffer).getUuid();
    }

    public UUID getUuid() {
        return uuid;
    }
}
