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

    public Scenario(String name, /*vorerst halt nur der dir name anstatt von ner config zu holen*/Path dir) {
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
                    String rgbString = entry.getValue().getAsString();
                    String[] parts = rgbString.split(";");
                    if (parts.length == 3) {
                        try {
                            int r = Integer.parseInt(parts[0].trim());
                            int g = Integer.parseInt(parts[1].trim());
                            int b = Integer.parseInt(parts[2].trim());
                            takenColors.add(new Color(r, g, b));
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

    public List<Color> getTakenColors() {
        return takenColors;
    }
}
