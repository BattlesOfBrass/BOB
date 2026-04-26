package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.UUID;

public class PlayerChangedCountryPacket implements Packet {

    private UUID uuid;
    private String countryAbbreviation;

    public PlayerChangedCountryPacket() {

    }

    public PlayerChangedCountryPacket(UUID uuid, String countryAbbreviation) {
        this.uuid = uuid;
        this.countryAbbreviation = countryAbbreviation;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil buf = BufferUtil.of(buffer);

        buf.putUuid(uuid);
        buf.putUtf(countryAbbreviation);
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil buf = BufferUtil.of(buffer);

        uuid = buf.getUuid();
        countryAbbreviation = buf.getUtf();
    }
}
