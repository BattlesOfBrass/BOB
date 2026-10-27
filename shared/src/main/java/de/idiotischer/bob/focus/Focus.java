package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;

import java.awt.*;
import java.util.Set;

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

    public Focus(String id, Set<Modifier> visibleWhen, int time, float ppDebuff, Point point, String prerequisite, Set<Modifier> modifiers, String description) {
        this.id = id;
        this.point = point;
        this.prerequisite = prerequisite;
        this.time = time;
        this.ppDebuff = ppDebuff; //apply as modifier as soon as the focus is taken and when cancelled, or completed rm
        this.modifiers = modifiers;
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

    //relative to prerequisite
    public Point getPoint() {
        return point;
    }

}
