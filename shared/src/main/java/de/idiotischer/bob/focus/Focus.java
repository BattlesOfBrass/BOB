package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.executable.Effect;
import de.idiotischer.bob.executable.Requirements;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.state.StateResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.time.TimeResolver;

import java.awt.*;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.stream.Collectors;

//TODO: add conditions that need to be fulfilled in order to do the focus (e.g. a specific time, ideology, war support, population, troop count etc)
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
    private final Set<String> exclusiveWith;
    private final int weight;
    private final Set<String> tags;
    private final Requirements requirements;

    public Focus(String id, String name, int weight, Set<String> tags, Set<String> doneFocuses, Set<String> exclusiveWith, Set<Modifier> visibleWhen, int time, float ppDebuff, Point point, String prerequisite, Requirements requirements, Effect effect, String description) {
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
        this.exclusiveWith = exclusiveWith;
        this.weight = weight;
        this.tags = tags;
        this.requirements = requirements;
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
                Set<Modifier> mods = value.stream().map(r2::by).filter(Objects::nonNull).collect(Collectors.toSet());

                if (abbr.equals("SELF")) abbr = c;
                for (Modifier mod : mods) cr.byAbbreviation(abbr).addModifierServer(t, r, mod, tr);
            });
            this.effect.modsRemove().forEach((abbr, value) -> {
                Set<Modifier> mods = value.stream().map(r2::by).filter(Objects::nonNull).collect(Collectors.toSet());

                if (abbr.equals("SELF")) abbr = c;
                for (Modifier mod : mods) cr.byAbbreviation(abbr).removeModifierServer(t, r, mod.getId());
            });
            this.effect.tilesTransferMap().forEach((key,value) -> {
                Country country = cr.byAbbreviation(key);
                if(country != null) value.forEach(s -> {
                    Tile tile = tir.byAbbreviation(s);
                    if(tile != null) {
                        tile.setControllerForAll(r.channels(), country);
                        tile.setOwnerForAll(r.channels(), country);
                    }
                });
            });
            this.effect.popMap().forEach((key,value) -> {
                Tile tile = tir.byAbbreviation(key);
                if(tile != null) tile.setPopulationServer(t,r,value);
            });
            this.effect.tilesAddCores().forEach((key,value) -> {
                Tile tile = tir.byAbbreviation(key);
                if(tile != null) value.forEach(s -> tile.addCoreServer(t,r,s));
            });
            this.effect.tilesRemoveCores().forEach((key,value) -> {
                Tile tile = tir.byAbbreviation(key);
                if(tile != null) value.forEach(s -> tile.removeCoreServer(t,r,s));
            });
            this.effect.tilesAddClaims().forEach((key,value) -> {
                Tile tile = tir.byAbbreviation(key);
                if(tile != null) value.forEach(s -> tile.addClaimServer(t,r,s));
            });
            this.effect.tilesRemoveClaims().forEach((key,value) -> {
                Tile tile = tir.byAbbreviation(key);
                if(tile != null) value.forEach(s -> tile.removeClaimServer(t,r,s));
            });
        }

        //idk why i broadcast this but for specs its cool ig)
        t.broadcast(r.channels(), new ReplyPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "end=" + c));
    }

    public void startFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new RequestPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "start=" + c));
    }

    public void cancelFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new RequestPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "cancel=" + c));
    }

    public void endFocusClient(AsynchronousSocketChannel r, SendTool t, String tree, String c) {
        t.send(r, new RequestPacket(Type.FOCUS_UPDATE, "id=" + this.id + ";" + "treeId=" + tree + ";" + "end=" + c));
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

    public Set<String> getExclusiveWith() {
        return exclusiveWith;
    }

    public boolean isHistorical() {
        return tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("historical"));
    }

    public int getWeight() {
        return weight;
    }

    //relative to prerequisite
    public Point getPoint() {
        return point;
    }

    public String toDataString() {
        StringBuilder visibleWhenSB = new StringBuilder();
        StringBuilder mapSB2 = new StringBuilder();

        StringBuilder sb3 = new StringBuilder();
        StringBuilder sb4 = new StringBuilder();

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

        var array4 = new ArrayList<>(exclusiveWith);

        for (int i = 0; i < array4.size(); i++) {
            String p = array4.get(i);
            sb3.append(p);

            if (i < array4.size() - 1) sb3.append("|");
        }


        var array5 = new ArrayList<>(tags);

        for (int i = 0; i < array5.size(); i++) {
            String p = array5.get(i);
            sb4.append(p);

            if (i < array5.size() - 1) sb4.append("|");
        }

        return id + ";" + name + ";" + prerequisite + ";" + point.getX() + "," + point.getY() + ";" + time + ";" + ppDebuff + ";" + visibleWhenSB + ";" + description + ";" + mapSB2 + ";" + sb3 + ";" + sb4 + ";" + weight + ";" + effect.toDataString() + ";" + (requirements != null ? requirements.toDataString() : "");
    }

    public static Focus fromDataString(String data, ModifierResolver r) {
        String[] strings = data.split(";", -1);

        if (strings.length != 14) {
            for (int i = 0; i < strings.length; i++) System.err.println(i + ": " + strings[i]);

            return null;
        }

        String id = strings[0];
        String name = strings[1];
        String prerequisite = strings[2];
        String[] xy = strings[3].split(",",2);
        int time = Integer.parseInt(strings[4]);
        float ppDebuff = Float.parseFloat(strings[5]);
        String visibleWhenString = strings[6];
        String desc = strings[7];
        String countriesDoneString = strings[8];
        String exclusive = strings[9];
        String tagString = strings[10];
        int weight = 0;
        String effectString = strings[12];
        String requirementsS = strings[13];

        try {
            weight = Integer.parseInt(strings[11]);
        } catch (NumberFormatException ignored) {}

        Point p = new Point((int) Double.parseDouble(xy[0]), (int) Double.parseDouble(xy[1]));

        Set<Modifier> visibleWhen = new HashSet<>();

        Set<String> exclusiveWith = new HashSet<>(Arrays.asList(exclusive.split("\\|")));

        if (!visibleWhenString.isEmpty()) Arrays.stream(visibleWhenString.split("\\|")).map(r::by).filter(Objects::nonNull).forEach(visibleWhen::add);

        Set<String> tags = new HashSet<>(Arrays.asList(tagString.split("\\|")));

        Effect effect = Effect.fromDataString(effectString);
        Requirements requirements = Requirements.fromDataString(requirementsS);

        Set<String> countriesDone = new HashSet<>(Arrays.asList(countriesDoneString.split("\\|")));

        return new Focus(id,name, weight, tags,countriesDone,exclusiveWith,visibleWhen,time,ppDebuff,p,prerequisite,requirements,effect,desc);
    }

    public Requirements getRequirements() {
        return requirements;
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
