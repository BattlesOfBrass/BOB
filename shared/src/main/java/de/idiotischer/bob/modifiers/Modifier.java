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
public class Modifier {

    private final String id;
    private final SendTool t;
    private final ChannelResolver r;

    private String displayName;
    private String specificallyFor = "";
    private float modifier;
    private String type;

    private static final Set<String> modifierTypes = new HashSet<>();

    static {
        modifierTypes.add("politicalPower");
        modifierTypes.add("manpower");
        modifierTypes.add("attack");
    }

    public Modifier(ChannelResolver r, SendTool t, String id, String displayName, float modifier, String type) {
        this(r,t,id,displayName,"",modifier,type);
    }

    public Modifier(String id, String displayName, float modifier, String type) {
        this(id,displayName,"",modifier,type);
    }

    public Modifier(String id, String displayName, String specificallyFor, float modifier, String type) {
        this(null,null,id,displayName,specificallyFor,modifier,type);
    }

    public Modifier(ChannelResolver r, SendTool t, String id, String displayName, String specificallyFor, float modifier, String type) {
        this.t = t;
        this.r = r;
        this.id = id;
        this.displayName = displayName;
        this.specificallyFor = specificallyFor;
        this.modifier = modifier;
        this.type = type;
    }

    public String toDataString() {
        return id + ";" + displayName + ";" + specificallyFor + ";" + modifier + ";" + type;
    }

    public static Modifier fromDataString(String data) {
        String[] strings = data.split(";");

        if(strings.length != 5) return null;

        return new Modifier(strings[0], strings[1], strings[2], Float.parseFloat(strings[3]), strings[4]);
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

    public float getModifier() {
        return modifier;
    }

    public void setModifier(float modifier) {
        this.modifier = modifier;
    }

    public void setModifierServer(float modifier) {
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
        ATTACK("attack");

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
