package de.idiotischer.bob.scenario;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.BOB;
import de.idiotischer.bob.SharedCore;

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

    private Scenario currentScenario = null;

    public void load(Scenario scenario) {
        currentScenario = scenario;
    }

    public BufferedImage getMap() {
        return currentScenario.getMapImage();
    }

    public List<Color> getTakenColors() {
        return currentScenario.getTakenColors();
    }

    public Path getScenariopath() {
        return currentScenario.getDir();
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }
}
