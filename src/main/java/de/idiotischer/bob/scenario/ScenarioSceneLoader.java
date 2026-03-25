package de.idiotischer.bob.scenario;

import de.idiotischer.bob.BOB;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

//maybe handle currentselected scenario here even though it's more of a local thing for the menu?
public class ScenarioSceneLoader {

    private Scenario currentScenario = null;

    public void load(Scenario scenario) {
        this.load(scenario, false);
    }

    public void load(Scenario scenario, boolean switchMM) {
        currentScenario = scenario;

        if(BOB.getInstance().getMainRenderer() == null) return;

        BOB.getInstance().getCountries().reload();

        if(switchMM) {
            BOB.getInstance().getMainRenderer().getGamePanel().setEscMenu(false);
            BOB.getInstance().getMainRenderer().setMainMenu(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
        }

        if(currentScenario.getMapImage() != null) {
            BOB.getInstance().getMainRenderer().setMap(currentScenario.getMapImage());

            SwingUtilities.invokeLater(() -> {
                if( BOB.getInstance().getMainRenderer().getGamePanel() == null) return;
                BOB.getInstance().getMainRenderer().getGamePanel().setFrame(scenario.getMapImage());
                BOB.getInstance().getMainRenderer().setZoom(BOB.getInstance().getMainRenderer().getMinZoom());
            });
        }
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
