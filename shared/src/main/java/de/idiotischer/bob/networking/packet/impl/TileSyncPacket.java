package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TileSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

    private Tile tile;

    private String abbreviation;
    private String name;
    private String countryAbbreviation;
    private String ownerAbbreviation;
    private List<Point> points;
    private boolean reconstructed = false;
    private String cityName;
    private boolean city;
    private int victoryPoints;
    private Set<String> claims = new HashSet<>();

    public TileSyncPacket() {}

    public TileSyncPacket(Tile tile) {
        this.tile = tile;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(tile.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        String info = BufferUtil.of(buffer).getUtf();

        String[] parts = info.split(";", -1);

        this.abbreviation = parts[0];
        this.name = parts[1];
        this.city = Boolean.parseBoolean(parts[2]);
        this.cityName = parts[3];

        this.points = new ArrayList<>();
        String[] pointParts = parts[4].split("\\|");

        for (String p : pointParts) {
            String[] coords = p.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            points.add(new Point(x, y));
        }

        this.countryAbbreviation = parts[5];
        this.ownerAbbreviation = parts[6];
        this.victoryPoints = Integer.parseInt(parts[7]);

        this.claims = new HashSet<>();

        if (!parts[8].isEmpty()) {
            this.claims.addAll(Arrays.asList(parts[8].split(",")));
        }
    }

    public void reconstruct(SharedCore core, CountryResolver resolver) {
        this.reconstruct(core, resolver, false);
    }

    public void reconstruct(SharedCore core, CountryResolver resolver, boolean forceReconstruct) {
        if(!forceReconstruct && reconstructed) return;

        this.tile = Tile.by(core, claims.stream().map(resolver::byAbbreviation).filter(Objects::nonNull).collect(Collectors.toSet()), resolver, abbreviation, victoryPoints, name, cityName, city, points, countryAbbreviation,ownerAbbreviation);

        reconstructed = true;
    }

    public Tile getTile() {
        return tile;
    }
}

//obsolote and handled dumb
//public class TileSyncPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {
//    private List<String> tiles;
//
//    //map aus Tile.toString()
//    public TileSyncPacket() {}
//
//    public TileSyncPacket(List<String> tileAndOwners) {
//        tiles = tileAndOwners;
//    }
//
//    @Override
//    public void write(BufferUtil buffer) {
//        buffer.getRaw().putInt(tiles.size());
//
//        for (String s : tiles) {
//            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
//            buffer.getRaw().putInt(bytes.length);
//
//            buffer.getRaw().put(bytes);
//        }
//    }
//
//    @Override
//    public void write(ByteBuffer buffer) {
//        buffer.putInt(tiles.size());
//
//        for (String s : tiles) {
//            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
//            buffer.putInt(bytes.length);
//
//            buffer.put(bytes);
//        }
//    }
//
//    @Override
//    public void read(ByteBuffer buffer) {
//        tiles = new ArrayList<>();
//
//        int size = buffer.getInt();
//
//        for (int i = 0; i < size; i++) {
//            int length = buffer.getInt();
//            byte[] bytes = new byte[length];
//            buffer.get(bytes);
//            tiles.add(new String(bytes, StandardCharsets.UTF_8));
//        }
//    }
//
//    @Override
//    public void handle(Networker networker) {
//
//    }
//}
