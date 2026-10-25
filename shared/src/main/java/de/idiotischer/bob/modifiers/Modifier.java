package de.idiotischer.bob.modifiers;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// TODO: outsource "specificallyFOr" (for example *Infantry* attack or *Motorised* attack are specific For)
// TODO: Sync modifier changes to the client
public class Modifier {

    private final String id;
    private String displayName;
    private String specificallyFor = "";
    private float modifier = 0;
    private String type;

    private static final Set<String> modifierTypes = new HashSet<>();

    static {
        modifierTypes.add("politicalPower");
        modifierTypes.add("manpower");
        modifierTypes.add("attack");
    }

    public Modifier(String id, String displayName, float modifier, String type) {
        this(id,displayName,"",modifier,type);
    }

    public Modifier(String id, String displayName, String specificallyFor, float modifier, String type) {
        this.id = id;
        this.displayName = displayName;
        this.specificallyFor = specificallyFor;
        this.modifier = modifier;
        this.type = type;
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

    public String getSpecificallyFor() {
        return specificallyFor;
    }

    public void setSpecificallyFor(String specificallyFor) {
        this.specificallyFor = specificallyFor;
    }

    public float getModifier() {
        return modifier;
    }

    public void setModifier(float modifier) {
        this.modifier = modifier;
    }

    public String getType() {
        return type;
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
