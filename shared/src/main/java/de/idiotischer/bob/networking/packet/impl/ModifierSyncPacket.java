package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.modifiers.Modifier;

import java.nio.ByteBuffer;

public class ModifierSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

    private Modifier modifier;


    public ModifierSyncPacket() {
    }

    public ModifierSyncPacket(Modifier modifier) {
        this.modifier = modifier;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(modifier.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        modifier = Modifier.fromDataString(BufferUtil.of(buffer).getUtf());
    }

    public Modifier getModifier() {
        return modifier;
    }
}
