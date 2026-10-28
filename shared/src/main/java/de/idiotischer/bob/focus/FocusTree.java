package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;

import java.util.*;

//static class, they shouldnt really change
public record FocusTree(String requiredTag, Set<String> countries, Set<Focus> focuses, String id /*, String name i'd just name the tree per country*/) {

    public boolean isSpecificForCountries() {
        return !countries().isEmpty();
    }

    public boolean hasTree(Country c) {
        return c.getModifiers().stream().anyMatch(mod -> mod.getModifier().equals(requiredTag)) && countries.contains(c.getAbbreviation());
    }

    public boolean isDefault() {
        return requiredTag.isEmpty();
    }

    public String toDataString() {
        StringBuilder countriesSB = new StringBuilder();
        StringBuilder focusesSB = new StringBuilder();

        var array = new ArrayList<>(countries);

        for (int i = 0; i < array.size(); i++) {
            String p = array.get(i);
            countriesSB.append(p);

            if (i < array.size() - 1) countriesSB.append("|");
        }

        var array2 = new ArrayList<>(focuses);

        for (int i = 0; i < array2.size(); i++) {
            Focus p = array2.get(i);
            focusesSB.append(p.toDataString());

            if (i < array2.size() - 1) focusesSB.append("|");
        }

        return id + ";" + requiredTag + ";" + countriesSB + ";" + focusesSB;
    }

    public static FocusTree fromDataString(String data, ModifierResolver r) {
       String[] parts = data.split(";",-1);

       String id = parts[0];
       String requiredTag = parts[1];

       Set<String> countries = new HashSet<>();
       if(!parts[2].isEmpty()) countries.addAll(Arrays.asList(parts[2].split("\\|")));

       Set<Focus> focuses = new HashSet<>();
       if(!parts[3].isEmpty())
           for(String focusDat : parts[3].split("\\|"))
               focuses.add(Focus.fromDataString(focusDat,r));

       return new FocusTree(requiredTag,countries,focuses,id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FocusTree other)) return false;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
