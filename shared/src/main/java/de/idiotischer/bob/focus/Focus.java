package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.effect.Effect;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.state.StateResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.time.TimeResolver;

import java.awt.*;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.stream.Collectors;

//these should be defined in a focustree directly but i think it'd be cool to have a global.json that registers focuses globally
public class Focus {

    private final String id;
    private final Point point;
    private final String prerequisite;
    private final int time;
    private final float ppDebuff;
    private final String description;
    private final Set<Modifier> visibleWhen;
    private final String name;
    private final Effect effect;
    private final Set<String> countryDone;

    public Focus(String id, String name, Set<String> doneFocuses, Set<Modifier> visibleWhen, int time, float ppDebuff, Point point, String prerequisite, Effect effect, String description) {
        this.id = id;
        this.name = name;
        this.point = point;
        this.prerequisite = prerequisite;
        this.time = time;
        this.ppDebuff = ppDebuff; //apply as modifier as soon as the focus is taken and when cancelled, or completed rm
        this.effect = effect;
        this.description = description;
        this.visibleWhen = visibleWhen;
        this.countryDone = doneFocuses;
    }

    public void startFocus(String c) {

    }

    public void cancelFocus(String c) {

    }

    public void endFocus(String c) {
        countryDone.add(c);
    }

    public void startFocusServer(ChannelResolver r, SendTool t, String tree, String c) {
        startFocus(c);

        //idk why i broadcast this but for specs its cool ig)
        t.broadcast(r.channels(), new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "start=" + c));
    }

    public void cancelFocusServer(ChannelResolver r, SendTool t, String tree, String c) {
        cancelFocus(c);

        //idk why i broadcast this but for specs its cool ig)
        t.broadcast(r.channels(), new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "cancel=" + c));
    }

    public void endFocusServer(StateResolver sr, TileResolver tir, ModifierResolver r2, CountryResolver cr, ChannelResolver r, SendTool t, String tree, String c, TimeResolver tr) {
        endFocus(c);

        if(effect != null) {
            this.effect.modsAdd().forEach((abbr, value) -> {
                Set<Modifier> mods = value.stream().map(r2::by).collect(Collectors.toSet());

                if (abbr.equals("SELF")) abbr = c;
                for (Modifier mod : mods) cr.byAbbreviation(abbr).addModifierServer(t, r, mod, tr);
            });
            this.effect.modsRemove().forEach((abbr, value) -> {
                Set<Modifier> mods = value.stream().map(r2::by).collect(Collectors.toSet());

                if (abbr.equals("SELF")) abbr = c;
                for (Modifier mod : mods) cr.byAbbreviation(abbr).removeModifierServer(t, r, mod.getId());
            });
            this.effect.tilesTransferMap().forEach((key,value) -> {
                Country country = cr.byAbbreviation(key);
                if(country != null) value.forEach(s -> {
                    Tile tile = tir.byAbbreviation(s);
                    tile.setControllerForAll(r.channels(), country);
                    tile.setOwnerForAll(r.channels(), country);
                });
            });
            this.effect.popMap().forEach((key,value) -> {
                State state = sr.resolve(key);

                if(state != null) {
                    //do stuff haha
                }
            });
        }

        //idk why i broadcast this but for specs its cool ig)
        t.broadcast(r.channels(), new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "end=" + c));
    }

    public void startFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "start=" + c));
    }

    public void cancelFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "cancel=" + c));
    }

    public void endFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "end=" + c));
    }

    public String getId() {
        return id;
    }

    public float getPPDebuff() {
        return ppDebuff;
    }

    public Effect getEffect() {
        return effect;
    }

    public int getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public String getPrerequisite() {
        return prerequisite;
    }

    public Set<Modifier> getVisibleWhen() {
        return visibleWhen;
    }

    public Set<String> getCountryDone() {
        return countryDone;
    }

    public boolean isVisibleFor(Country c) {
        return c.getModifiers().containsAll(visibleWhen);
    }

    public String getName() {
        return name;
    }

    //relative to prerequisite
    public Point getPoint() {
        return point;
    }

    public String toDataString() {
        StringBuilder visibleWhenSB = new StringBuilder();
        StringBuilder mapSB2 = new StringBuilder();

        var array2 = new ArrayList<>(visibleWhen);

        for (int i = 0; i < array2.size(); i++) {
            Modifier p = array2.get(i);
            visibleWhenSB.append(p.getId());

            if (i < array2.size() - 1) visibleWhenSB.append("|");
        }
        var array3 = new ArrayList<>(countryDone);

        for (int i = 0; i < array3.size(); i++) {
            String p = array3.get(i);
            mapSB2.append(p);

            if (i < array3.size() - 1) mapSB2.append("|");
        }


        return id + ";" + name + ";" + prerequisite + ";" + point.getX() + "," + point.getY() + ";" + time + ";" + ppDebuff + ";" + visibleWhenSB + ";" + description + ";" + mapSB2 + ";" + effect.toDataString();
    }

    public static Focus fromDataString(String data, ModifierResolver r) {
        String[] strings = data.split(";");

        if(strings.length != 10) return null;

        String id = strings[0];
        String name = strings[1];
        String prerequisite = strings[2];
        String[] xy = strings[3].split(",",2);
        int time = Integer.parseInt(strings[4]);
        float ppDebuff = Float.parseFloat(strings[5]);
        String visibleWhenString = strings[6];
        String desc = strings[7];
        String countriesDoneString = strings[8];
        String effectString = strings[9];

        Point p = new Point(Integer.parseInt(xy[0]),Integer.parseInt(xy[1]));

        Set<Modifier> visibleWhen = new HashSet<>();

        Arrays.stream(visibleWhenString.split("\\|")).forEach(s -> visibleWhen.add(r.by(s)));

        Effect effect = Effect.fromDataString(effectString);

        Set<String> countriesDone = new HashSet<>(Arrays.asList(countriesDoneString.split("\\|")));

        return new Focus(id,name,countriesDone,visibleWhen,time,ppDebuff,p,prerequisite,effect,desc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Focus other)) return false;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
