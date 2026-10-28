package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;

import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

//these should be defined in a focustree directly but i think it'd be cool to have a global.json that registers focuses globally
public class Focus {

    private final String id;
    private final Point point;
    private final String prerequisite;
    private final int time;
    private final float ppDebuff;
    private final Set<Modifier> modifiers;
    private final String description;
    private final Set<Modifier> visibleWhen;
    private final Map<String, Set<Modifier>> modifiersForOthers;
    private final String name;
    //add map of focuses that are done in this tree Map<String, Set<String>> and sync at the start + read from config too

    public Focus(String id, String name, Map<String, Set<Modifier>> modifiersForOthers, Set<Modifier> visibleWhen, int time, float ppDebuff, Point point, String prerequisite, Set<Modifier> modifiers, String description) {
        this.id = id;
        this.name = name;
        this.point = point;
        this.prerequisite = prerequisite;
        this.time = time;
        this.ppDebuff = ppDebuff; //apply as modifier as soon as the focus is taken and when cancelled, or completed rm
        this.modifiers = modifiers;
        this.modifiersForOthers = modifiersForOthers; //apply to others when focus taken
        this.description = description;
        this.visibleWhen = visibleWhen;
    }

    public String getId() {
        return id;
    }

    public float getPPDebuff() {
        return ppDebuff;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
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

    public boolean isVisibleFor(Country c) {
        return c.getModifiers().containsAll(visibleWhen);
    }

    public Map<String, Set<Modifier>> getModifiersForOthers() {
        return modifiersForOthers;
    }

    public String getName() {
        return name;
    }

    //relative to prerequisite
    public Point getPoint() {
        return point;
    }

    public String toDataString() {
        StringBuilder modifiersSB = new StringBuilder();
        StringBuilder visibleWhenSB = new StringBuilder();
        StringBuilder mapSB = new StringBuilder();

        var array = new ArrayList<>(modifiers);

        for (int i = 0; i < array.size(); i++) {
            Modifier p = array.get(i);
            modifiersSB.append(p.getId());

            if (i < array.size() - 1) modifiersSB.append("|");
        }

        var array2 = new ArrayList<>(visibleWhen);

        for (int i = 0; i < array2.size(); i++) {
            Modifier p = array2.get(i);
            visibleWhenSB.append(p.getId());

            if (i < array2.size() - 1) visibleWhenSB.append("|");
        }

        for(var entry : this.modifiersForOthers.entrySet()) {
            if(!mapSB.isEmpty()) mapSB.append(",");

            //"...;key1:value11|value12,key2:value21|value22

            mapSB.append(entry.getKey()).append(":");
            mapSB.append(String.join("|",entry.getValue().stream().map(Modifier::getId).collect(Collectors.toSet())));
        }

        return id + ";" + name + ";" + prerequisite + ";" + point.getX() + "," + point.getY() + ";" + time + ";" + ppDebuff + ";" + modifiersSB + ";" + visibleWhenSB + ";" + mapSB + ";" + description;
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
        String mods = strings[6];
        String visibleWhenString = strings[7];
        String mapString = strings[8];
        String desc = strings[9];

        Point p = new Point(Integer.parseInt(xy[0]),Integer.parseInt(xy[1]));

        Set<Modifier> visibleWhen = new HashSet<>();
        Set<Modifier> modifiers = new HashSet<>();

        Arrays.stream(mods.split("\\|")).forEach(s -> modifiers.add(r.by(s)));
        Arrays.stream(visibleWhenString.split("\\|")).forEach(s -> visibleWhen.add(r.by(s)));

        Map<String, Set<Modifier>> modifierMap = new HashMap<>();

        for(String entry : mapString.split(",")) {
            String[] keyV = entry.split(":",2);

            if(keyV.length != 2) continue;

            String key = keyV[0];

            Set<Modifier> modifs = Arrays.stream(keyV[1].split("\\|")).map(r::by).collect(Collectors.toSet());
            modifierMap.put(key,modifs);
        }

        return new Focus(id,name,modifierMap,visibleWhen,time,ppDebuff,p,prerequisite,modifiers,desc);
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
