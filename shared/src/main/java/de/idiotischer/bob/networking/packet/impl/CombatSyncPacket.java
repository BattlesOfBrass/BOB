package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.combat.CombatStatus;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.troop.TroopResolver;

import java.nio.ByteBuffer;

public class CombatSyncPacket implements Packet {
    private TroopResolver resolver;
    private CombatStatus status;

    private String deserialized;

    public CombatSyncPacket() {}

    public CombatSyncPacket(CombatStatus status, TroopResolver resolver) {
        this.status = status;
        this.resolver = resolver;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(status.toDataString(resolver));
    }

    @Override
    public void read(ByteBuffer buffer) {
        deserialized = BufferUtil.of(buffer).getUtf();
    }

    public CombatStatus getStatus(TroopResolver resolver) {
        return CombatStatus.fromDataString(deserialized, resolver);
    }
}
