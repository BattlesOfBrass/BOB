package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;

public class IdeologySyncPacket implements Packet {

    private Ideology ideology;

    public IdeologySyncPacket() {}

    public IdeologySyncPacket(Ideology ideology) {
        this.ideology = ideology;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(ideology.serialize());
    }

    @Override
    public void read(ByteBuffer buffer) {
        this.ideology = Ideology.deserialize(BufferUtil.of(buffer).getUtf());
    }

    public Ideology getIdeology() {
        return ideology;
    }
}
