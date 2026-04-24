package de.idiotischer.bob.scenario;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Scenario {

    private final String name;
    private final Path dir;

    private final List<Color> takenColors = new ArrayList<>();
    private final String abbreviation;
    private final boolean server;
    private List<Color> borderColors = new  ArrayList<>();

    public Scenario(boolean server, String abbreviation, String name, Path dir) {
        this.server = server;
        this.abbreviation = abbreviation;
        this.name = name;
        this.dir = dir;

        loadTaken();
    }

    public String getName() {
        return name;
    }

    public Path getDir() {
        return dir;
    }

    public Path getUnusable() {
        Path path = dir.resolve("unusable.json");

        if(Files.notExists(path)) path = FileUtil.getDefaultScenarioDir().resolve("unusable.json");

        return path;
    }

    public Path getCountryConfig() {
        Path path = dir.resolve("countries.json");

        if(Files.notExists(path)) path = FileUtil.getDefaultScenarioDir().resolve("countries.json");

        return path;
    }

    public Path getStatesConfig() {
        Path path = dir.resolve("states.json");

        if(Files.notExists(path)) path = FileUtil.getDefaultScenarioDir().resolve("states.json");

        return path;
    }

    public Path getMap() {
        Path path = dir.resolve("map.png");

        if(Files.notExists(path)) path = FileUtil.getDefaultScenarioDir().resolve("map.png");

        return path;
    }

    public Path getBackground() {
        Path path = dir.resolve("background.png");

        if(Files.notExists(path)) path = FileUtil.getDefaultScenarioDir().resolve("background.png");

        return path;
    }

    public boolean isUnusableDefault() {
        Path path = dir.resolve("unusable.json");

        return Files.notExists(path);
    }

    public boolean isCountryConfigDefault() {
        Path path = dir.resolve("countries.json");

        return Files.notExists(path);
    }

    public boolean isStatesConfigDefault() {
        Path path = dir.resolve("states.json");

        return Files.notExists(path);
    }

    public boolean isMapDefault() {
        Path path = dir.resolve("map.png");

        return Files.notExists(path);
    }

    public boolean isBackgroundDefault() {
        Path path = dir.resolve("background.png");

        return Files.notExists(path);
    }

    public BufferedImage getBackgroundImage() {
        Path path = getBackground();

        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            return  null;
        }
    }

    public BufferedImage getMapImage() {
        Path path = getMap();

        try {
            return ImageIO.read(path.toFile());
        } catch (IOException e) {
            return  null;
        }
    }

    public boolean loadTaken() {
        takenColors.clear();
        if (getUnusable() == null || !getUnusable().toFile().exists()) {
            return false;
        }

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(getUnusable()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            if (root.isJsonObject()) {
                root.getAsJsonObject().entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    String rgbString = entry.getValue().getAsString();
                    String[] parts = rgbString.split(";");
                    if (parts.length == 3) {
                        try {
                            int r = Integer.parseInt(parts[0].trim());
                            int g = Integer.parseInt(parts[1].trim());
                            int b = Integer.parseInt(parts[2].trim());

                            Color c = new Color(r, g, b);

                            if(key.contains("land_border")) borderColors.add(c);

                            takenColors.add(c);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                });
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public List<Color> getTakenColors() {
        return takenColors;
    }

    public boolean isServer() {
        return server;
    }

    public List<Color> getBorderColors() {
        return borderColors;
    }
}