package de.idiotischer.bob.faction;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Faction {

    private final String abbreviation;
    private final Set<String> members;
    private final Set<FactionRule> rules;

    private String owner;
    private SendTool t;

    private String name;

    private ChannelResolver r;

    public Faction(Set<FactionRule> factionRules, String abbreviation, String name, String owner, Set<String> members) {
        this(null,null,factionRules,abbreviation,name,owner,members);
    }

    public Faction(SendTool t, ChannelResolver r, Set<FactionRule> factionRules, String abbreviation, String name, String owner, Set<String> members) {
        members.add(owner);
        this.abbreviation = abbreviation;
        this.name = name;
        this.owner = owner;
        this.members = members;
        this.rules = factionRules;
        this.r = r;
        this.t = t;
    }

    public void addMemberServer(String country) {
        this.members.add(country);
        //broadcast
        if(r == null || t == null) return;

        t.broadcast(r.channels(), new ReplyPacket(Type.FACTION_UPDATE, "id=" + abbreviation + ";" + "addMember=" + country));
    }

    public void rmMemberServer(String country) {
        this.members.remove(country);
        //broadcAST
        if(r == null || t == null) return;

        t.broadcast(r.channels(), new ReplyPacket(Type.FACTION_UPDATE, "id=" + abbreviation + ";" + "removeMember=" + country));
    }

    public void addMember(String country) {
        this.members.add(country);
    }

    public void rmMember(String country) {
        this.members.remove(country);
    }

    public void serverSetName(String name) {
        this.name = name;

        //broadcast
        t.broadcast(r.channels(), new ReplyPacket(Type.FACTION_UPDATE, "id=" + abbreviation + ";" + "changeName=" + name));
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void changeOwnerServer(String owner) {
        this.owner = owner;
        //broadcast
        t.broadcast(r.channels(), new ReplyPacket(Type.FACTION_UPDATE, "id=" + abbreviation + ";" + "newOwner=" + owner));
    }

    public String toDataString() {
        StringBuilder sb = new StringBuilder();

        var array = new ArrayList<>(rules);

        for (int i = 0; i < rules.size(); i++) {
            FactionRule p = array.get(i);
            sb.append(p.id());

            if (i < array.size() - 1) sb.append("|");
        }

        StringBuilder sb1 = new StringBuilder();

        var array1 = new ArrayList<>(members);

        for (int i = 0; i < members.size(); i++) {
            String p = array1.get(i);
            sb1.append(p);

            if (i < array1.size() - 1) sb1.append("|");
        }

        return abbreviation + ";" + name + ";" + owner + ";" + sb + ";" + sb1;
    }

    public static Faction fromDataString(FactionResolver r, String data) {
        String[] parts = data.split(";", -1);

        if(parts.length != 5) return null;

        String abbr = parts[0];
        String name = parts[1];
        String owner = parts[2];

        String[] ruleParts = parts[3].split("\\|");
        Set<FactionRule> rules = new HashSet<>();

        for (String ruleString : ruleParts) rules.add(r.ruleBy(ruleString));

        String[] memParts = parts[4].split("\\|");

        Set<String> members = new HashSet<>(Arrays.asList(memParts));

        return new Faction(rules,abbr,name,owner,members);
    }

    //TODO: later make people able to register a new type (for now its not important)
    //TODO: SYNC THEM TO THE CLIENT BEFORE FACTIONS ARE INITED
    //shouldnt update at all its static
    public record FactionRule(String id, String name, String category, Set<Modifier> modifiers) {

        public static FactionRule fromDataString(String data, ModifierResolver r) {
            String[] strings = data.split(";");

            if(strings.length != 4) return null;

            String id = strings[0];
            String name = strings[1];
            String category = strings[2];
            String mods = strings[3];

            Set<Modifier> modifiers = new HashSet<>();

            Arrays.stream(mods.split("\\|")).forEach(s -> modifiers.add(r.by(s)));

            return new FactionRule(id,name,category,modifiers);
        }

        public String toDataString() {
            StringBuilder sb = new StringBuilder();

            var array = new ArrayList<>(modifiers);

            for (int i = 0; i < modifiers.size(); i++) {
                Modifier p = array.get(i);
                sb.append(p.getId());

                if (i < array.size() - 1) sb.append("|");
            }

            return id + ";" + name + ";" + category + ";" + sb;
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Faction other)) return false;

        return Objects.equals(abbreviation, other.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abbreviation);
    }
}
