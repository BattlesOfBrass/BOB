package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.tile.Tile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TilesSyncPacket implements Packet {

    private List<TileSyncPacket> packets = new ArrayList<>();

    public TilesSyncPacket() {}

    public TilesSyncPacket(List<TileSyncPacket> packets) {
        this.packets = packets;
    }

    public static TilesSyncPacket fromTiles(Set<Tile> tiles) {
        return new TilesSyncPacket(tiles.stream().map(TileSyncPacket::new).toList());
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
            TileSyncPacket packet = new TileSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<TileSyncPacket> getPackets() {
        return packets;
    }
}
