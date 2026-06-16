package de.idiotischer.bob;

import de.idiotischer.bob.country.CountryManager;
import de.idiotischer.bob.debug.Debugger;
import de.idiotischer.bob.game.GameManager;
import de.idiotischer.bob.listener.PacketListener;
import de.idiotischer.bob.networking.ClientSocket;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.player.PlayerManager;
import de.idiotischer.bob.render.MainRenderer;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.scenario.ScenarioSceneLoader;
import de.idiotischer.bob.state.StateManager;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileManager;
import de.idiotischer.bob.troop.Troop;
import de.idiotischer.bob.troop.TroopManager;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;
import de.idiotischer.bob.war.WarManager;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

public class BOB {

    private static BOB instance;

    private CountryManager countries;

    private MainRenderer mapRenderer;

    private TileManager tileManager;

    private Player player;

    private PlayerManager playerManager;

    private Debugger debugger;

    private final ScenarioSceneLoader scenarioSceneLoader =
        new ScenarioSceneLoader();

    private ClientSocket client;

    private SharedCore sharedCore;

    private ScenarioManager scenarioManager;

    private MainConfigUtil config;

    private boolean isHost = false;

    private TroopManager troopManager;

    private WarManager warManager;

    private Server localServer;
    private boolean remoteConnected = false;
    private CompletableFuture<Void> awaitingReload;
    private boolean initialized = false;
    private GameManager gameManager;
    private StateManager stateManager;

    public static void main(String[] args) {
        new BOB();
    }

    public BOB() {
        System.setProperty("app.name", "BOB");
        Thread.currentThread().setName(System.getProperty("app.name"));

        BOB.instance = this;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.client.shutdown()));

        FileUtil.replaceIfNotExistingAsync(
            this.getClass().getClassLoader()
        ).join();

        //TODO: fix this called before all files are created (thenRun doesnt work)
        init();
    }

    public void setup() {
        if (countries.getCountries().isEmpty()) {
            throw new IllegalStateException("Setup called before countries loaded");
        }

        this.mapRenderer = new MainRenderer();

        this.debugger = new Debugger();

        this.mapRenderer.start();

        this.initialized = true;
    }

    public void init() {
        config = new MainConfigUtil();

        FileUtil.getScenarioDir();

        this.sharedCore = new SharedCore();

        this.sharedCore.getListenerRegistry().register(new PacketListener());

        this.localServer = new Server(true);

        this.playerManager = new PlayerManager();

        this.client = new ClientSocket();

        this.scenarioManager = new ScenarioManager();

        this.countries = new CountryManager();

        this.tileManager = new TileManager();

        this.stateManager = new StateManager();

        this.troopManager = new TroopManager();

        this.warManager = new WarManager();

        this.awaitingReload = this.scenarioManager.reload().thenRun(() -> {
            this.scenarioSceneLoader.requestScenarioLoad(scenarioManager.getRandom());
        });

        this.gameManager = new GameManager();
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

    public CountryManager getCountryManager() {
        return countries;
    }

    public static BOB getInstance() {
        return BOB.instance;
    }

    public MainRenderer getMainRenderer() {
        return mapRenderer;
    }

    public ScenarioSceneLoader getScenarioSceneLoader() {
        return scenarioSceneLoader;
    }

    public TileManager getTileManager() {
        return tileManager;
    }

    public Player getPlayer() {
        return player;
    }

    public SendTool getSendTool() {
        return sharedCore.getTool();
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

    public void setHost(boolean b) {
        this.isHost = b;
    }

    public TroopManager getTroopManager() {
        return troopManager;
    }

    //hier ist das für scenarios syncen (alle), remtoe scenarioen werden einzeln gefetched
    public Server getLocalServer() {
        return localServer;
    }

    //und mit remote dann das für den rest also länder etc was jeder server braucht


    public StateManager getStateManager() {
        return stateManager;
    }

    public CompletableFuture<Void> getAwaitingReload() {
        return awaitingReload;
    }

    public boolean isRemoteConnected() {
        return remoteConnected;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public MainConfigUtil getConfig() {
        return config;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public void setPlayer(UUID uuid) {
        this.player = playerManager.createPlayer(client.getChannel(), uuid, AddressUtil.getThisAddress(client.getChannel()));
    }
}
