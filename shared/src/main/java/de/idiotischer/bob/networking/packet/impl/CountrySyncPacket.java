package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.ideology.IdeologyResolver;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;

public class CountrySyncPacket implements Packet {

    private Country country;

    private ByteBuffer buffer;

    public CountrySyncPacket() {}

    public CountrySyncPacket(Country country) {
        this.country = country;
    }

    @Override
    public void write(ByteBuffer buffer) {
        country.writeCountry(buffer);
    }

    @Override
    public void read(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public Country getCountry(IdeologyResolver r) {
        return Country.readCountry(r,buffer);
    }
}
