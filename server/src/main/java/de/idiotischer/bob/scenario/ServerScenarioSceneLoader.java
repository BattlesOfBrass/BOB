package de.idiotischer.bob.scenario;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.networking.packet.impl.ScenarioSyncPacket;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Path;
import java.util.List;

//maybe handle currentselected scenario here even though it's more of a local thing for the menu?
public class ServerScenarioSceneLoader {

    private Scenario currentScenario = null;

    public void load() {
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ScenarioSyncPacket(currentScenario));
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

    public void loadFor(AsynchronousSocketChannel channel) {
        Server.getInstance().getSendTool().send(channel, new ScenarioSyncPacket(currentScenario));
    }

    public void loadNew(Scenario scenario) {
        currentScenario = scenario;

        Server.getInstance().getCountryManager().reload();
        Server.getInstance().getStateManager().reload();

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ScenarioSyncPacket(scenario));
    }
}