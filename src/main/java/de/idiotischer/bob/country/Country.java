package de.idiotischer.bob.country;

import de.idiotischer.bob.BOB;

import java.awt.*;
import java.util.Set;
import java.util.stream.Collectors;

public class Country {

    private final String name;
    private final Color color;
    private final String abbreviation;
    private boolean major = false;

    public Country(String abbreviation, String name, Color color, boolean major) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.color = color;
        this.major = major;
    }

    public Color countryColor() {
        return color;
    }

    public boolean exists() {
        return true;
    }

    public String countryName() {
        return name;
    }

    // --> testweise immer gleich
    public Relations countryRelations() {
        return null;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public static Country fromJson(String json) {
        return null;
    }

    public boolean isFree() {
        return true;
    }

    public PuppetState getPuppetState() {
        return null;
    }

    /*wie nah oder nicht nah man am puppet werden, bzw am level demoten ist*/
    public int puppetProgress() {
        return -1; //nicht puppetable
    }

    public boolean isAutonomous() {
        return true;
    }

    public boolean isMajor() {
        return major;
    }

    public void setMajor(boolean major) {
        this.major = major;
    }

    public Set<Country> getPuppets() {
        return BOB.getInstance().getCountries().getCountrySet().stream().filter(c -> !c.isFree()).collect(Collectors.toSet());
    }

    public static Country fromNameExact(String name) {
        return BOB.getInstance().getCountries().getCountrySet().stream().filter(country -> country.countryName().equals(name)).findFirst().orElse(null);
    }

    public static Country fromAbbreviation(String abbreviation) {
        return BOB.getInstance().getCountries().getCountrySet().stream().filter(country -> country.getAbbreviation().equals(abbreviation.toUpperCase())).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromColor(Color color) {
        return BOB.getInstance().getCountries().getCountrySet().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromPixel(int x, int y) {
        Color color = null;//color getten
        return BOB.getInstance().getCountries().getCountrySet().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", color=" + color +
                ", abbreviation='" + abbreviation + '\'' +
                '}';
    }
}
