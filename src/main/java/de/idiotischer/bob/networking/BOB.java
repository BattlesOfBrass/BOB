package de.idiotischer.bob.networking;

import de.idiotischer.bob.networking.country.Countries;
import de.idiotischer.bob.networking.debug.Debugger;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.player.LocalPlayer;
import de.idiotischer.bob.networking.player.Player;
import de.idiotischer.bob.networking.render.MainRenderer;
import de.idiotischer.bob.networking.scenario.ScenarioSceneLoader;
import de.idiotischer.bob.networking.state.StateManager;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class BOB {
    private static BOB instance;

    private Countries countries;

    private MainRenderer mapRenderer;

    private StateManager stateManager;

    private Player player;

    private Debugger debugger;

    private Path configs;

    private final ScenarioSceneLoader scenarioSceneLoader = new ScenarioSceneLoader();

    private ClientSocket client;

    private SharedCore sharedCore;

    private SendTool sendTool;

    private boolean isHost = false;

    static void main() {
        new BOB();
    }

    public BOB() {
        BOB.instance = this;

        try {
            configs = Paths.get(Objects.requireNonNull(getClass()
                    .getClassLoader()
                    .getResource("config/")).toURI());

            URI testURI = Objects.requireNonNull(getClass()
                    .getClassLoader()
                    .getResource("scenario/ww1/")).toURI();

            scenarioSceneLoader.load(testURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();

            System.exit(1);

            return;
        }

        init();
    }

    public void init() {
        this.sharedCore = new SharedCore();

        this.sendTool = new SendTool(sharedCore);

        this.countries = new Countries();
        this.stateManager = new StateManager();

        this.client = new ClientSocket();

        this.player = new LocalPlayer(countries.getRandom());

        this.mapRenderer = new MainRenderer(player);

        debugger = new Debugger();

        //davor (vor start) ensuren dass der scenescenarioloader halt geladen hat
        if (mapRenderer != null) {
            mapRenderer.start();
        }
    }

    public ImageIcon createIcon() {
        URL imgURL = Objects.requireNonNull(getClass()
                .getClassLoader()
                .getResource("icons/icon.png"));
        if(imgURL != null)
            return new ImageIcon(imgURL);
        else
            return null;
    }
    public Countries getCountries() {
        return countries;
    }

    public static BOB getInstance() {
        return BOB.instance;
    }

    public MainRenderer getMapRenderer() {
        return mapRenderer;
    }

    public ScenarioSceneLoader getScenarioSceneLoader() {
        return scenarioSceneLoader;
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public Player getPlayer() {
        return player;
    }

    public SendTool getSendTool() {
        return sendTool;
    }

    public Path getConfigPath() {
        return configs;
    }

    public void setConfigPath(Path configPath) {
        this.configs = configPath;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public ClientSocket getClient() {
        return client;
    }

    public SharedCore getSharedCore() {
        return sharedCore;
    }

    public boolean save() {
        return true;
    }
}
