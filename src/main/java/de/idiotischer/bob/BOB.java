package de.idiotischer.bob;

import de.idiotischer.bob.country.CountryManager;
import de.idiotischer.bob.debug.Debugger;
import de.idiotischer.bob.listener.PacketListener;
import de.idiotischer.bob.networking.ClientSocket;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.player.PlayerManager;
import de.idiotischer.bob.player.ServerPlayer;
import de.idiotischer.bob.render.MainRenderer;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.scenario.ScenarioSceneLoader;
import de.idiotischer.bob.state.StateManager;
import de.idiotischer.bob.troop.TroopManager;
import de.idiotischer.bob.util.AdressUtil;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

public class BOB {

    private static BOB instance;

    private CountryManager countries;

    private MainRenderer mapRenderer;

    private StateManager stateManager;

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

    private Server localServer;
    private boolean remoteConnected = false;
    private CompletableFuture<Void> awaitingReload;
    private boolean initialized = false;

    public static void main(String[] args) {
        new BOB();
    }

    public BOB() {
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

        this.player = playerManager.createPlayer(AdressUtil.getClientAddress(client.getChannel()));

        this.playerManager.addPlayer(player);

        this.playerManager.changeCountry(player, countries.getRandom());

        this.mapRenderer = new MainRenderer(player);

        this.debugger = new Debugger();

        this.mapRenderer.start();

        this.initialized = true;
    }

    public void init() {
        config = new MainConfigUtil();

        FileUtil.getScenarioDir();

        this.sharedCore = new SharedCore();

        sharedCore.getListenerRegistry().register(new PacketListener());

        this.localServer = new Server(true);

        this.playerManager = new PlayerManager();

        this.client = new ClientSocket();

        this.scenarioManager = new ScenarioManager();

        this.countries = new CountryManager();

        this.stateManager = new StateManager();

        this.troopManager = new TroopManager();

        this.awaitingReload = this.scenarioManager.reload().thenRun(() -> {
            this.scenarioSceneLoader.requestScenarioLoad(scenarioManager.getRandom());
        });
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

    public StateManager getStateManager() {
        return stateManager;
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

    public CompletableFuture<Void> getAwaitingReload() {
        return awaitingReload;
    }

    public boolean isRemoteConnected() {
        return remoteConnected;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}
