package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.faction.FactionResolver;
import de.idiotischer.bob.tile.Tile;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class FactionSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

    private Faction faction;

    private String factionS;

    public FactionSyncPacket() {}

    public FactionSyncPacket(Faction faction) {
        this.faction = faction;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(faction.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        factionS = BufferUtil.of(buffer).getUtf();
    }

    public Faction getFaction(FactionResolver r) {
        return Faction.fromDataString(r,factionS);
    }
}