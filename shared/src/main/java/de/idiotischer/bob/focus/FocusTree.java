package de.idiotischer.bob.focus;

import de.idiotischer.bob.country.Country;

import java.util.Set;

//static class, they shouldnt really change
public record FocusTree(String requiredTag, Set<String> countries, Set<String> focuses, String id /*, String name i'd just name the tree per country*/) {

    public boolean isSpecificForCountries() {
        return !countries().isEmpty();
    }

    public boolean hasTree(Country c) {
        return c.getModifiers().stream().anyMatch(mod -> mod.getModifier().equals(requiredTag)) && countries.contains(c.getAbbreviation());
    }

    public boolean isDefault() {
        return requiredTag.isEmpty();
    }
}
