package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.troop.TroopStack;
import it.unimi.dsi.fastutil.Pair;

import java.nio.ByteBuffer;
import java.util.UUID;

public class TroopStackSyncPacket implements Packet {

    private UUID uuid;
    private TroopStack troop;

    private String deserialized;

    public TroopStackSyncPacket() {}

    public TroopStackSyncPacket(UUID uuid, TroopStack state) {
        this.uuid = uuid;
        this.troop = state;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(troop.serialize(uuid));
    }

    @Override
    public void read(ByteBuffer buffer) {
        deserialized = BufferUtil.of(buffer).getUtf();
    }

    public Pair<UUID,TroopStack> getTroopStack(CountryResolver countryResolver, TileResolver tileResolver) {
        return TroopStack.deserialize(deserialized,countryResolver,tileResolver);
    }

    public UUID getUuid() {
        return uuid;
    }
}
