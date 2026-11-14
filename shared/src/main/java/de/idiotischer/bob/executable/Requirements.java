
package de.idiotischer.bob.executable;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.idiotischer.bob.SharedCore;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public record Requirements(Map<String, Set<String>> atWarWith, Map<String, Set<String>> atPeaceWith, Map<String, Set<String>> controlsTiles, Map<String, Set<String>> ownsTiles, Map<String, Set<String>> needsIdeology, Map<String, Set<String>> hasModifier, Map<String, Set<String>> hasNotModifier, Set<String> isDateLessOrMore, Map<String, Set<String>> moreOrLessTroops, boolean shouldBePuppet, boolean shouldNotBePuppet, Map<String, Set<String>> hasPuppets) {

    public static Requirements fromJson(JsonObject obj) {
        Type mapStringSetType = new TypeToken<Map<String, Set<String>>>() {}.getType();

        Type stringSetType = new TypeToken<Set<String>>() {}.getType();

        Map<String, Set<String>> atWarWith = new HashMap<>();
        Map<String, Set<String>> atPeaceWith = new HashMap<>();
        Map<String, Set<String>> controlsTiles = new HashMap<>();
        Map<String, Set<String>> ownsTiles = new HashMap<>();
        Map<String, Set<String>> needsIdeology = new HashMap<>();
        Set<String> isDateLessOrMore = new HashSet<>();
        Map<String, Set<String>> moreOrLessTroops = new HashMap<>();
        Map<String, Set<String>> hasModifier = new HashMap<>();
        Map<String, Set<String>> hasNotModifier = new HashMap<>();

        if (obj.has("atWarWith")) {
            JsonElement el = obj.get("atWarWith");
            if (el != null) atWarWith = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("atPeaceWith")) {
            JsonElement el = obj.get("atPeaceWith");
            if (el != null) atPeaceWith = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("controlsTiles")) {
            JsonElement el = obj.get("controlsTiles");
            if (el != null) controlsTiles = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("ownsTiles")) {
            JsonElement el = obj.get("ownsTiles");
            if (el != null) ownsTiles = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("needsIdeology")) {
            JsonElement el = obj.get("needsIdeology");
            if (el != null) needsIdeology = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("isDateLessOrMore")) {
            JsonElement el = obj.get("isDateLessOrMore");
            if (el != null) isDateLessOrMore = SharedCore.GSON.fromJson(el, stringSetType);
        }

        if (obj.has("moreOrLessTroops")) {
            JsonElement el = obj.get("moreOrLessTroops");
            if (el != null) moreOrLessTroops = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        if (obj.has("hasModifier")) {
            JsonElement el = obj.get("hasModifier");
            if (el != null) hasModifier = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        //ig combine these two

        if (obj.has("hasNotModifier")) {
            JsonElement el = obj.get("hasNotModifier");
            if (el != null) hasNotModifier = SharedCore.GSON.fromJson(el, mapStringSetType);
        }

        boolean shouldBePuppet = obj.has("shouldBePuppet") && obj.get("shouldBePuppet").getAsBoolean();
        boolean shouldNotBePuppet = obj.has("shouldNotBePuppet") && obj.get("shouldNotBePuppet").getAsBoolean();

        Map<String, Set<String>> hasPuppets = new HashMap<>();

        return new Requirements(atWarWith, atPeaceWith, controlsTiles, ownsTiles, needsIdeology, hasModifier, hasNotModifier, isDateLessOrMore, moreOrLessTroops,shouldBePuppet, shouldNotBePuppet, hasPuppets);
    }

    public String toDataString() {
        StringBuilder atWarBuilder = new StringBuilder();
        StringBuilder atPeaceBuilder = new StringBuilder();
        StringBuilder controlsTilesBuilder = new StringBuilder();
        StringBuilder ownsTilesBuilder = new StringBuilder();
        StringBuilder ideologyBuilder = new StringBuilder();
        StringBuilder dateBuilder = new StringBuilder();
        StringBuilder troopsBuilder = new StringBuilder();
        StringBuilder hasModifierBuilder = new StringBuilder();
        StringBuilder hasNotModifierBuilder = new StringBuilder();
        StringBuilder hasPuppetsBuilder = new StringBuilder();

        for (var entry : atWarWith.entrySet()) {
            if (!atWarBuilder.isEmpty()) atWarBuilder.append(",");
            atWarBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : atPeaceWith.entrySet()) {
            if (!atPeaceBuilder.isEmpty()) atPeaceBuilder.append(",");
            atPeaceBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : controlsTiles.entrySet()) {
            if (!controlsTilesBuilder.isEmpty()) controlsTilesBuilder.append(",");
            controlsTilesBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : ownsTiles.entrySet()) {
            if (!ownsTilesBuilder.isEmpty()) ownsTilesBuilder.append(",");
            ownsTilesBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : needsIdeology.entrySet()) {
            if (!ideologyBuilder.isEmpty()) ideologyBuilder.append(",");
            ideologyBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (String date : isDateLessOrMore) {
            if (!dateBuilder.isEmpty()) dateBuilder.append(",");
            dateBuilder.append(date);
        }

        for (var entry : moreOrLessTroops.entrySet()) {
            if (!troopsBuilder.isEmpty()) troopsBuilder.append(",");
            troopsBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : hasModifier.entrySet()) {
            if (!hasModifierBuilder.isEmpty()) hasModifierBuilder.append(",");
            hasModifierBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : hasNotModifier.entrySet()) {
            if (!hasNotModifierBuilder.isEmpty()) hasNotModifierBuilder.append(",");
            hasNotModifierBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        for (var entry : hasPuppets.entrySet()) {
            if (!hasPuppetsBuilder.isEmpty()) hasPuppetsBuilder.append(",");
            hasPuppetsBuilder.append(entry.getKey()).append(":").append(String.join("|", entry.getValue()));
        }

        return atWarBuilder + "#" + atPeaceBuilder + "#" + controlsTilesBuilder + "#" + ownsTilesBuilder + "#" + ideologyBuilder + "#" + dateBuilder + "#" + troopsBuilder + "#" + hasModifierBuilder + "#" + hasNotModifierBuilder + "#" + shouldBePuppet + "#" + shouldNotBePuppet + "#" + hasPuppetsBuilder;
    }

    public String formatRequirements() {
        StringBuilder sb = new StringBuilder();

        append(sb, "At war with", this.atWarWith());
        append(sb, "At peace with", this.atPeaceWith());
        append(sb, "Has modifier", this.hasModifier());
        append(sb, "Does not have modifier", this.hasNotModifier());
        append(sb, "Controls tiles", this.controlsTiles());
        append(sb, "Owns tiles", this.ownsTiles());
        append(sb, "Needs ideology", this.needsIdeology());
        append(sb, "Troop requirements", this.moreOrLessTroops());

        if (this.isDateLessOrMore() != null && !this.isDateLessOrMore().isEmpty()) {
            sb.append("Date requirements:\n");

            for (String requirement : this.isDateLessOrMore()) sb.append("  • ").append(requirement).append("\n");
        }

        if (shouldBePuppet) sb.append("Must be a puppet.\n");

        if (shouldNotBePuppet) sb.append("Must not be a puppet.\n");

        append(sb, "Has puppets", this.hasPuppets());

        if (sb.isEmpty()) return "No requirements.";

        return sb.toString().trim();
    }

    public boolean fulfillsPuppetCheck(boolean isPuppet) {
        if (shouldBePuppet && !isPuppet) return false;

        return !shouldNotBePuppet || !isPuppet;
    }

    private void append(StringBuilder sb, String title, Map<String, Set<String>> requirements) {
        if (requirements == null || requirements.isEmpty()) return;

        sb.append(title).append(":\n");

        requirements.forEach((key, values) -> {
            sb.append("  • ").append(key);

            if (values != null && !values.isEmpty()) sb.append(": ").append(String.join(" OR ", values));

            sb.append("\n");
        });
    }

    public static Requirements fromDataString(String data) {
        String[] strings = data.split("#", -1);

        if (strings.length != 9 && strings.length != 12) return null;

        Map<String, Set<String>> atWarWith = parseMap(strings[0]);
        Map<String, Set<String>> atPeaceWith = parseMap(strings[1]);
        Map<String, Set<String>> controlsTiles = parseMap(strings[2]);
        Map<String, Set<String>> ownsTiles = parseMap(strings[3]);
        Map<String, Set<String>> needsIdeology = parseMap(strings[4]);

        Set<String> isDateLessOrMore = Arrays.stream(strings[5].split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        Map<String, Set<String>> moreOrLessTroops = parseMap(strings[6]);
        Map<String, Set<String>> hasModifier = parseMap(strings[7]);
        Map<String, Set<String>> hasNotModifier = parseMap(strings[8]);

        boolean shouldBePuppet = false;
        boolean shouldNotBePuppet = false;
        Map<String, Set<String>> hasPuppets = new HashMap<>();

        if (strings.length == 12) {
            shouldBePuppet = Boolean.parseBoolean(strings[9]);
            shouldNotBePuppet = Boolean.parseBoolean(strings[10]);
            hasPuppets = parseMap(strings[11]);
        }

        return new Requirements(atWarWith, atPeaceWith, controlsTiles, ownsTiles, needsIdeology, hasModifier, hasNotModifier, isDateLessOrMore, moreOrLessTroops, shouldBePuppet, shouldNotBePuppet, hasPuppets);
    }

    private static Map<String, Set<String>> parseMap(String data) {
        Map<String, Set<String>> result = new HashMap<>();

        for (String entry : data.split(",")) {
            String[] keyV = entry.split(":", 2);

            if (keyV.length != 2) continue;

            String key = keyV[0];

            Set<String> values = Arrays.stream(keyV[1].split("\\|")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());

            result.put(key, values);
        }

        return result;
    }


    public boolean fulfillsDateCheck(Date date) {
        if (isDateLessOrMore.isEmpty()) return true;

        long value = date.getTime();

        boolean rangeMatches = true;
        boolean equalityMatches = false;
        boolean hasEquality = false;

        for (String condition : isDateLessOrMore) {
            try {
                char operator = condition.charAt(0);
                long target = Long.parseLong(condition.substring(1));

                switch (operator) {
                    case '=' -> {
                        hasEquality = true;
                        equalityMatches |= value == target;
                    }
                    case '>' -> rangeMatches &= value > target;
                    case '<' -> rangeMatches &= value < target;
                    default -> {return false;}
                }
            } catch (Exception ignored) {
                return false;
            }
        }

        return rangeMatches || (hasEquality && equalityMatches);
    }

    public boolean fulfillsTroopsCheck(String country, int troops) {
        Set<String> conditions = moreOrLessTroops.get(country);

        if (conditions == null || conditions.isEmpty()) return true;

        for (String condition : conditions) {
            try {
                char operator = condition.charAt(0);
                int target = Integer.parseInt(condition.substring(1));

                switch (operator) {
                    case '=' -> {if (troops != target) return false;}
                    case '>' -> {if (troops <= target) return false;}
                    case '<' -> {if (troops >= target) return false;}
                }
            } catch (Exception ignored) {
                return false;
            }
        }

        return true;
    }

}
