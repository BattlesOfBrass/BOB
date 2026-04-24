package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.state.State;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CountriesSyncPacket implements Packet {

    private List<CountrySyncPacket> packets = new ArrayList<>();

    public CountriesSyncPacket() {}

    public CountriesSyncPacket(List<CountrySyncPacket> packets) {
        this.packets = packets;
    }

    public static CountriesSyncPacket fromCountries(List<Country> countries) {
        return new CountriesSyncPacket(countries.stream().map(CountrySyncPacket::new).toList());
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(packets.size());

        packets.forEach(packet -> packet.write(buffer));
    }

    @Override
    public void read(ByteBuffer buffer) {
        packets.clear();

        int size = buffer.getInt();
        for (int i = 0; i < size; i++) {
            CountrySyncPacket packet = new CountrySyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<CountrySyncPacket> getPackets() {
        return packets;
    }
}
