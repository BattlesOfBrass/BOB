package de.idiotischer.bob;

import de.idiotischer.bob.country.CountryManager;
import de.idiotischer.bob.debug.Debugger;
import de.idiotischer.bob.networking.ClientSocket;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.player.LocalPlayer;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.render.MainRenderer;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.scenario.ScenarioSceneLoader;
import de.idiotischer.bob.state.StateManager;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class BOB {
    private static BOB instance;

    private CountryManager countries;

    private MainRenderer mapRenderer;

    private StateManager stateManager;

    private Player player;

    private Debugger debugger;

    private final ScenarioSceneLoader scenarioSceneLoader = new ScenarioSceneLoader();

    private ClientSocket client;

    private SharedCore sharedCore;

    private SendTool sendTool;

    private ScenarioManager scenarioManager;

    private MainConfigUtil config;

    private boolean isHost = false;

    static void main() {
        new BOB();
    }

    public BOB() {
        BOB.instance = this;

        FileUtil.replaceIfNotExistingAsync(this.getClass().getClassLoader()).join();
        init();
    }

    public void init() {
        config = new MainConfigUtil();
        this.sharedCore = new SharedCore();

        this.sendTool = new SendTool(sharedCore);

        this.scenarioManager = new ScenarioManager();

        this.scenarioSceneLoader.load(scenarioManager.getRandom());

        this.countries = new CountryManager();

        this.stateManager = new StateManager();

        this.client = new ClientSocket();

        this.player = new LocalPlayer(countries.getRandom());

        this.mapRenderer = new MainRenderer(player);

        this.debugger = new Debugger();

        //davor (vor start) ensuren dass der scenescenarioloader halt geladen hat
        this.mapRenderer.start();
    }

    public ImageIcon createIcon() {
        URL imgURL;

        try {
            imgURL = FileUtil.getIconPath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return new ImageIcon(imgURL);
    }
    public CountryManager getCountries() {
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

    public boolean isDebug() {
        return config.isDebug();
    }

    public boolean isHost() {
        return isHost;
    }

    public ScenarioManager getScenarioManager() {
        return scenarioManager;
    }

}
