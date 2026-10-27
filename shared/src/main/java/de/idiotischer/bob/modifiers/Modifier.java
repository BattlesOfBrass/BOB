package de.idiotischer.bob.modifiers;

import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// TODO: outsource "specificallyFOr" (for example *Infantry* attack or *Motorised* attack are specific For)
// TODO: Sync modifier changes to the client
// A modifier can be direct (since i'm lazy, so if the modifiers "lasts" is 0 than its directly applied 1x
// made into a string to also act as country tag
// add a removemodifier type so modifiers can be created for removing other modifiers (they are put into the add method, than discarded and the modifier that is in the modifier field is removed)
public class Modifier {

    private final String id;
    private final SendTool t;
    private final ChannelResolver r;

    private String displayName;
    private String specificallyFor = "";
    private String modifier;
    private final int lasts;
    private boolean display;
    private String type;

    private static final Set<String> modifierTypes = new HashSet<>();

    static {
        modifierTypes.add("politicalPower");
        modifierTypes.add("manpower");
        modifierTypes.add("attack");
        modifierTypes.add("removeModifier");
    }

    public Modifier(ChannelResolver r, SendTool t, String id, String displayName, boolean display, int lasts, String modifier, String type) {
        this(r,t,id,displayName,display, lasts,"",modifier,type);
    }

    public Modifier(String id, String displayName, boolean display, int lasts, String modifier, String type) {
        this(id,displayName,display,lasts,"",modifier,type);
    }

    public Modifier(String id, String displayName, boolean display, int lasts, String specificallyFor, String modifier, String type) {
        this(null,null,id,displayName,display,lasts,specificallyFor,modifier,type);
    }

    public Modifier(ChannelResolver r, SendTool t, String id, String displayName, boolean display, int lasts, String specificallyFor, String modifier, String type) {
        this.t = t;
        this.r = r;
        this.id = id;
        this.displayName = displayName;
        this.specificallyFor = specificallyFor;
        this.modifier = modifier;
        this.type = type;
        this.lasts = lasts;
        this.display = display;
    }

    public String toDataString() {
        return id + ";" + displayName + ";" + specificallyFor + ";" + display + ";" + modifier + ";" + type + ";" + lasts;
    }

    public static Modifier fromDataString(String data) {
        String[] strings = data.split(";");

        if(strings.length != 7) return null;

        return new Modifier(strings[0], strings[1], Boolean.parseBoolean(strings[3]), Integer.parseInt(strings[6]), strings[2], strings[4], strings[5]);
    }

    //idk if this should be settable
    public int getLasts() {
        return lasts;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplayNameServer(String displayName) {
        this.displayName = displayName;
        t.broadcast(r.channels(), new ReplyPacket(Type.MODIFIER_UPDATE, "id=" + id + ";" + "displayName=" + displayName));
    }

    public String getSpecificallyFor() {
        return specificallyFor;
    }

    public void setSpecificallyFor(String specificallyFor) {
        this.specificallyFor = specificallyFor;
    }

    public void setSpecificallyForServer(String specificallyFor) {
        this.specificallyFor = specificallyFor;
        t.broadcast(r.channels(), new ReplyPacket(Type.MODIFIER_UPDATE, "id=" + id + ";" + "specificallyFor=" + displayName));
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public void setModifierServer(String modifier) {
        this.modifier = modifier;
        t.broadcast(r.channels(), new ReplyPacket(Type.MODIFIER_UPDATE, "id=" + id + ";" + "modifier=" + modifier));

    }

    public String getType() {
        return type;
    }

    public void setTypeServer(String type) {
        this.type = type;
        t.broadcast(r.channels(), new ReplyPacket(Type.MODIFIER_UPDATE, "id=" + id + ";" + "type=" + type));
    }

    public void setType(String type) {
        this.type = type;
    }

    public static Set<String> getModifierTypes() {
        return modifierTypes;
    }

    public enum Types {
        POLITICAL_POWER("politicalPower"),
        MANPOWER("manpower"),
        ATTACK("attack"),
        REMOVE_MODIFIER("removeModifier");

        private final String mapping;

        Types(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Modifier other)) return false;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
