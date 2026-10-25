package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.TileResolver;

import java.nio.ByteBuffer;

public class StateSyncPacket implements Packet {

    private State state;

    private String deserialized;

    public StateSyncPacket() {}

    public StateSyncPacket(State state) {
        this.state = state;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(state.serialize());
    }

    @Override
    public void read(ByteBuffer buffer) {
        deserialized = BufferUtil.of(buffer).getUtf();
    }

    public String getDeserialized() {
        return deserialized;
    }

    public State getState(CountryResolver r, TileResolver t) {
        return State.deserialize(getDeserialized(), r, t);
    }
}
