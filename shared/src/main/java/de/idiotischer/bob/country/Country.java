package de.idiotischer.bob.country;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.UUID;

public class Country {

    private final String name;
    private final Color color;
    private final String abbreviation;
    private final boolean selectScreen;
    private boolean major;
    private Player player = null;
    private PuppetState puppetState;
    private int puppetProgress = 0; //in prozent (0-100) bzw maybe als float idk

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

    public void setPuppetState(PuppetState state) {
        this.puppetState = state;
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
        //buffer.put((byte) (getPuppetState() == null ? -1 : getPuppetState().ordinal()));

        buffer.putInt(getPuppetProgress());
    }

    public static Country readCountry(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        String name = util.getUtf();
        String abbreviation = util.getUtf();

        Color color = new Color(
                buffer.get() & 0xFF,
                buffer.get() & 0xFF,
                buffer.get() & 0xFF
        );

        boolean major = buffer.get() == 1;
        boolean selectScreen = buffer.get() == 1;

        UUID uuid = util.getUuid();
        if (uuid != null && uuid.equals(new UUID(0L, 0L))) {
            uuid = null;
        }

        PuppetState puppetState = util.getEnum(PuppetState.class);
        if (puppetState == PuppetState.NONE) {
            puppetState = null;
        }

        int puppetProgress = buffer.getInt();

        Country country = new Country(abbreviation, name, color, major, selectScreen);

        country.setPlayer(Player.of(uuid));
        country.setPuppetState(puppetState);
        country.setPuppetProgress(puppetProgress);

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
                ", puppetState=" + (getPuppetState() == null ? "null" : getPuppetState().name()) +
                ", exists=" + exists() +
                /*", autonomous=" + isAutonomous() + wird im client bestimmt*/
                '}';
    }
}
