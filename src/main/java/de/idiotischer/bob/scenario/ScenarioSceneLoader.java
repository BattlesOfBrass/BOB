package de.idiotischer.bob.scenario;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ScenarioSceneLoader {
    private final Gson gson = new Gson();

    private Path mapPath;
    private Path takenColorsPath;
    private Path scenariopath;

    private BufferedImage map;
    private final List<Color> takenColors = new ArrayList<>(); //dreamwastaken oder so

    //outsource this and yeah, it's a bit messy bc of testing purposes
    public void load(URI uri) {
        this.takenColors.clear();

        this.scenariopath = Paths.get(uri).resolve("ww1");

        this.mapPath = scenariopath.resolve("map.png");
        this.takenColorsPath = scenariopath.resolve("unusable.json"); //hardcoded for niw

        if(Files.notExists(this.mapPath)) {
            System.exit(0);
            return;
        }

        if(Files.notExists(this.takenColorsPath)) {
            this.takenColorsPath = scenariopath.resolve("../default/unusable.json");

            if(Files.notExists(this.takenColorsPath)) {
                System.exit(0);
                return;
            }
        }

        if(!loadMap()) return;

        loadTaken();
    }

    public void load(Path path) {
        load(path.toUri());
    }

    public boolean loadTaken() {
        if (takenColorsPath == null || !takenColorsPath.toFile().exists()) {
            return false;
        }

        try (JsonReader reader = new JsonReader(new FileReader(takenColorsPath.toFile()))) {
            JsonElement root = gson.fromJson(reader, JsonElement.class);

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

    public boolean loadMap() {
        try {
            map = ImageIO.read(mapPath.toFile());

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public BufferedImage getMap() {
        return map;
    }

    public List<Color> getTakenColors() {
        return takenColors;
    }

    public Path getScenariopath() {
        return scenariopath;
    }

    public Gson getGson() {
        return gson;
    }
}
