package de.idiotischer.bob.scenario;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;

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

        BOB.getInstance().getCountryManager().reload();

        if(switchMM) {
            BOB.getInstance().getMainRenderer().getGamePanel().setEscMenu(false);
            BOB.getInstance().getMainRenderer().setMainMenu(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelect(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new ScenarioSelectMenu(scenario));
        }

        if(currentScenario.getMapImage() != null) {
            BOB.getInstance().getMainRenderer().setMap(currentScenario.getMapImage());

            SwingUtilities.invokeLater(() -> {
                if( BOB.getInstance().getMainRenderer().getGamePanel() == null) return;
                BOB.getInstance().getMainRenderer().setMap(scenario.getMapImage());
                BOB.getInstance().getMainRenderer().getCamera().zoomToMin();
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
