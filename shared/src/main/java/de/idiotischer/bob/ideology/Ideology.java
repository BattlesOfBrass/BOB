package de.idiotischer.bob.ideology;

public record Ideology(String abbreviation, String name, Buffs buffs) {


    public record Buffs(float stabilityGainBuff, float politicalPowerGainBuff,float justifyWarGoalBuff) { }

    public String serialize() {
        return String.join(";", abbreviation, name, String.valueOf(buffs.stabilityGainBuff()), String.valueOf(buffs.politicalPowerGainBuff()), String.valueOf(buffs.justifyWarGoalBuff()));
    }

    public static Ideology deserialize(String s) {
        if (s == null) throw new IllegalArgumentException("Ideology string somehow null");

        String[] parts = s.split(";", -1);

        //if (parts.length != Ideology.class.getRecordComponents().length + Buffs.class.getRecordComponents().length) throw new IllegalArgumentException("Invalid ideology format, expected 5 fields, got " + parts.length);

        return new Ideology(parts[0], parts[1], new Buffs(Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), Float.parseFloat(parts[4])));
    }
}
