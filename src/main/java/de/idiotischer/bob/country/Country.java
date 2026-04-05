package de.idiotischer.bob.country;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class Country {

    private final String name;
    private final Color color;
    private final String abbreviation;
    private final boolean selectScreen;
    private boolean major;

    public Country(String abbreviation, String name, Color color, boolean major, boolean selectScreen) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.color = color;
        this.major = major;
        this.selectScreen = selectScreen;
    }

    //später nicht die default sondern die current flag returnen
    public Path getFlag() {
        return FileUtil.getFlag(abbreviation);
    }

    public BufferedImage getFlagImage() {
        try {
            return ImageIO.read(FileUtil.getFlag(abbreviation).toFile());
        } catch (IOException e) {
            return null;
        }
    }

    public Path getDefaultFlag() {
        return FileUtil.getFlag(abbreviation);
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

    public boolean isSelectScreen() {
        return selectScreen;
    }

    public void setMajor(boolean major) {
        this.major = major;
    }

    public Set<Country> getPuppets() {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(c -> !c.isFree()).collect(Collectors.toSet());
    }

    public static Country fromNameExact(String name) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryName().equals(name)).findFirst().orElse(null);
    }

    public static Country fromAbbreviation(String abbreviation) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.getAbbreviation().equals(abbreviation.toUpperCase())).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromColor(Color color) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromPixel(int x, int y) {
        Color color = null;//color getten
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    public Player getPlayer() {
        return null;
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
