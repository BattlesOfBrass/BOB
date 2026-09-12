package de.idiotischer.bob.country;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.util.FileUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class Country /*implements Comparable<Country>*/{

    private final String name;
    private final Color color;
    private final String abbreviation;
    private final boolean selectScreen;
    private boolean major;
    private Player player = null;
    private PuppetState puppetState;
    private int puppetProgress = 0; //in prozent (0-100) bzw maybe als float idk
    private Set<State> states = new HashSet<>();
    private boolean capitulated;
    private Country overlord = null;
    private Set<String> milAccess = new HashSet<>();

    public Country(String abbreviation, String name, Color color, boolean major, boolean selectScreen) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.color = color;
        this.major = major;
        this.selectScreen = selectScreen;
        this.capitulated = false;
    }

    //später nicht die default sondern die current flag returnen
    public Path getFlag(Scenario scenario) {
        return FileUtil.getFlag(scenario,abbreviation);
    }

    public BufferedImage getFlagImage(Scenario scenario) {
        var path = FileUtil.getFlag(scenario, abbreviation);
        if(path == null) return null;

        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    public Path getDefaultFlag(Scenario scenario) {
        return FileUtil.getFlag(scenario, abbreviation);
    }

    public Color countryColor() {
        return color;
    }

    public String countryName() {
        return name;
    }

    // --> testweise immer gleich
    public Relations countryRelations() {
        return null;
    }

    //later add ideology thingy to the abbreviation (make a sanatized and unsanaitzed or smth that allows for civil wars betrween the same ideology and yeah, cosmetictags or by leader or smth)
    public String getAbbreviation() {
        return abbreviation;
    }

    public static Country fromJson(String json) {
        return null;
    }

    public PuppetState getPuppetState() {
        return puppetState;
    }

    /*wie nah oder nicht nah man am puppet werden, bzw am level demoten ist*/
    public int getPuppetProgress() {
        return puppetProgress; //nicht puppetable
    }

    public boolean isAutonomous() {
        return getPuppetState() == null || getPuppetState() == PuppetState.NONE;
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

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setOverlord(Country country) {
        this.overlord = country;
    }

    public void setPuppetTile(PuppetState tile) {
        this.puppetState = tile;
    }

    public void setPuppetProgress(int progress) {
        this.puppetProgress = progress;
    }

    public void writeCountry(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        util.putUtf(name);
        util.putUtf(abbreviation);

        Color c = color;
        buffer.put((byte) c.getRed());
        buffer.put((byte) c.getGreen());
        buffer.put((byte) c.getBlue());

        buffer.put((byte) (major ? 1 : 0));
        buffer.put((byte) (selectScreen ? 1 : 0));

        if (getPlayer() == null || getPlayer().uuid() == null) {
            util.putUuid(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        } else {
            util.putUuid(getPlayer().uuid());
        }

        util.putEnum(getPuppetState() == null ? PuppetState.NONE : getPuppetState());
        //buffer.put((byte) (getPuppetTile() == null ? -1 : getPuppetTile().ordinal()));

        buffer.putInt(getPuppetProgress());
        BufferUtil.of(buffer).putBoolean(isCapitulated());

        buffer.putInt(milAccess.size());
        for (String country : milAccess) util.putUtf(country);
    }

    public static Country readCountry(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        String name = util.getUtf();
        String abbreviation = util.getUtf();

        Color color = new Color(buffer.get() & 0xFF, buffer.get() & 0xFF, buffer.get() & 0xFF);

        boolean major = buffer.get() == 1;
        boolean selectScreen = buffer.get() == 1;

        UUID uuid = util.getUuid();
        if (uuid != null && uuid.equals(new UUID(0L, 0L))) {
            uuid = null;
        }

        PuppetState puppetTile = util.getEnum(PuppetState.class);
        if (puppetTile == PuppetState.NONE) {
            puppetTile = null;
        }

        int puppetProgress = buffer.getInt();

        Country country = new Country(abbreviation, name, color, major, selectScreen);

        country.setPlayer(Player.of(uuid));
        country.setPuppetTile(puppetTile);
        country.setPuppetProgress(puppetProgress);
        country.setCapitulated(BufferUtil.of(buffer).getBoolean());

        int milAccessSize = buffer.getInt();
        for (int i = 0; i < milAccessSize; i++) country.addMilAccess(util.getUtf());

        return country;
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", color={" + "red=" + color.getRed() + ", green=" + color.getGreen() + ", blue=" + color.getBlue() + "}" +
                ", abbreviation='" + abbreviation + '\'' +
                ", major=" + major +
                ", selectScreen=" + selectScreen +
                ", player=" + (getPlayer() == null ? "null" : getPlayer().uuid() == null ? "null" : getPlayer().uuid().toString()) +
                ", getPuppetProgress=" + getPuppetProgress() +
                ", puppetTile=" + (getPuppetState() == null ? "null" : getPuppetState().name()) +
                ", capitulated=" + isCapitulated() +
                /*", autonomous=" + isAutonomous() + wird im client bestimmt*/
                '}';
    }

    public void addState(State state) {
        states.add(state);
    }

    public Set<State> getStates() {
        return states;
    }

    public boolean isCapitulated() {
        return capitulated;
    }

    public void setCapitulated(boolean b) {
        this.capitulated = b;
    }

    public void setCapitulated(boolean b, Consumer<Void> consumer) {
        this.capitulated = b;
        consumer.accept(null);
    }

    public Country getOverlord() {
        return overlord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country other)) return false;

        return Objects.equals(abbreviation, other.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abbreviation);
    }

    //returns the abbr
    public String getIdeology() {
        return "";
    }

    public void removeMilAccess(String country) {
        milAccess.remove(country);
    }

    public void addMilAccess(String country) {
        milAccess.add(country);
    }

    public boolean hasCountryMilAccess(Country country) {
        return milAccess.contains(country.getAbbreviation()); //TODO: or is in faction
    }

    public String serializeAccessUpdate(String abbr, boolean added) {
        return this.getAbbreviation() + ";" +abbr + ";" + added;
    }

    public static Pair<String, Pair<String, Boolean>> getAccessUpdate(String s) {
        String[] strings = s.split(";");

        String cAbbr = strings[0];
        String milCAbbr = strings[1];
        boolean added = Boolean.parseBoolean(strings[2]);

        return Pair.of(cAbbr, Pair.of(milCAbbr,added));
    }
    //@Override
    //public int compareTo(@NonNull Country o) {
    //    return this.abbreviation.compareTo(o.getAbbreviation());
    //}
}
