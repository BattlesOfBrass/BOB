package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.faction.FactionResolver;
import de.idiotischer.bob.modifiers.ModifierResolver;

import java.nio.ByteBuffer;

public class FactionRuleSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

    private Faction.FactionRule factionRule;

    private String factionS;

    public FactionRuleSyncPacket() {}

    public FactionRuleSyncPacket(Faction.FactionRule factionRule) {
        this.factionRule = factionRule;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(factionRule.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        factionS = BufferUtil.of(buffer).getUtf();
    }

    public Faction.FactionRule getFactionRule(ModifierResolver r) {
        return Faction.FactionRule.fromDataString(factionS, r);
    }
}