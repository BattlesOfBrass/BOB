package de.idiotischer.bob.scenario;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//maybe handle currentselected scenario here even though it's more of a local thing for the menu?
//TODO: Serialize image pixel as either black or white (boolean true/false) to safe bandwidth) but keep the option to use the more bandwidth heavy option ~ credits: Crafts_army
public class ScenarioSceneLoader {

    private Scenario currentScenario = null;
    private CompletableFuture<Void> awaitingFuture;

    public CompletableFuture<Void> requestScenarioLoad(Scenario scenario) {
        awaitingFuture = new CompletableFuture<>();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.SCENARIO_LOAD, scenario.getAbbreviation()));

        return awaitingFuture;
    }

    public void load(Scenario scenario, boolean setMap) {
        currentScenario = scenario;

        if(setMap) BOB.getInstance().getMainRenderer().setMap(this.currentScenario.getMapImage());

        //TODO: bald nur noch fetchen
        BOB.getInstance().getCountryManager().reload();

        if (awaitingFuture != null && !awaitingFuture.isDone()) {
            awaitingFuture.complete(null);
        }
    }

    public BufferedImage getMap() {
        return currentScenario.getMapImage();
    }

    public List<Color> getTakenColors() {
        return currentScenario.getTakenColors();
    }

    public List<Color> getBorderColors() {
        return currentScenario.getBorderColors();
    }

    public Path getScenariopath() {
        return currentScenario.getDir();
    }

    public Scenario getCurrentScenario() {
        return currentScenario;
    }

    public CompletableFuture<Void> getAwaitingFuture() {
        return awaitingFuture;
    }
}
