package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.state.State;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class StateSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

    private State state;

    private String abbreviation;
    private String name;
    private String countryAbbreviation;
    private List<Point> points;
    private boolean reconstructed = false;

    public StateSyncPacket() {}

    public StateSyncPacket(State state) {
        this.state = state;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(state.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        String info = BufferUtil.of(buffer).getUtf();

        String[] parts = info.split(";");

        this.abbreviation = parts[0];
        this.name = parts[1];

        this.points = new ArrayList<>();
        String[] pointParts = parts[2].split("\\|");

        for (String p : pointParts) {
            String[] coords = p.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            points.add(new Point(x, y));
        }

        this.countryAbbreviation = parts[3];
    }

    public void reconstruct(SharedCore core, CountryResolver resolver) {
        this.reconstruct(core, resolver, false);
    }

    public void reconstruct(SharedCore core, CountryResolver resolver, boolean forceReconstruct) {
        if(!forceReconstruct && reconstructed) return;

        this.state = State.by(core, resolver, abbreviation, name, points, countryAbbreviation);

        reconstructed = true;
    }

    public State getState() {
        return state;
    }
}

//obsolote and handled dumb
//public class StateSyncPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {
//    private List<String> states;
//
//    //map aus State.toString()
//    public StateSyncPacket() {}
//
//    public StateSyncPacket(List<String> stateAndOwners) {
//        states = stateAndOwners;
//    }
//
//    @Override
//    public void write(BufferUtil buffer) {
//        buffer.getRaw().putInt(states.size());
//
//        for (String s : states) {
//            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
//            buffer.getRaw().putInt(bytes.length);
//
//            buffer.getRaw().put(bytes);
//        }
//    }
//
//    @Override
//    public void write(ByteBuffer buffer) {
//        buffer.putInt(states.size());
//
//        for (String s : states) {
//            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
//            buffer.putInt(bytes.length);
//
//            buffer.put(bytes);
//        }
//    }
//
//    @Override
//    public void read(ByteBuffer buffer) {
//        states = new ArrayList<>();
//
//        int size = buffer.getInt();
//
//        for (int i = 0; i < size; i++) {
//            int length = buffer.getInt();
//            byte[] bytes = new byte[length];
//            buffer.get(bytes);
//            states.add(new String(bytes, StandardCharsets.UTF_8));
//        }
//    }
//
//    @Override
//    public void handle(Networker networker) {
//
//    }
//}
