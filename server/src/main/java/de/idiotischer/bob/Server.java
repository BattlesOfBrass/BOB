package de.idiotischer.bob;

import de.idiotischer.bob.country.ServerCountryManager;
import de.idiotischer.bob.listener.ServerPacketListener;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.player.ServerPlayerManager;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ServerScenarioManager;
import de.idiotischer.bob.scenario.ServerScenarioSceneLoader;
import de.idiotischer.bob.state.ServerStateManager;
import de.idiotischer.bob.state.StateValidator;
import de.idiotischer.bob.track.MapTracker;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;
import it.unimi.dsi.fastutil.ints.IntSets;

public class Server {

    private static Server instance;
    private final SharedCore core = new SharedCore();
    private ServerSocket serverSocket;
    private MainConfigUtil config;
    private ServerScenarioManager scenarioManager;
    private ServerCountryManager countryManager;
    private ServerScenarioSceneLoader scenarioLoader;
    private MapTracker mapTracker;
    private ServerStateManager stateManager;
    private StateValidator stateValidator = new StateValidator();
    private ServerPlayerManager playerManager;

    public static void main(String[] args) {
        new Server(false);
    }

    public Server(boolean local) {
        instance = this;

        FileUtil.replaceIfNotExistingAsync(
            this.getClass().getClassLoader()
        ).join();

        System.out.println("Server started");

        registerListeners();

        //FileUtil.getAllScenarios().forEach(scenario -> {System.out.println(scenario);});

        init(local);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {this.serverSocket.shutdown();}));
    }

    public void init(boolean local) {
        this.config = new MainConfigUtil();

        this.serverSocket = new ServerSocket(local);

        this.mapTracker = new MapTracker();

        this.playerManager = new ServerPlayerManager();

        this.scenarioManager = new ServerScenarioManager();
        this.scenarioManager.reload();

        this.scenarioLoader = new ServerScenarioSceneLoader();

        this.countryManager = new ServerCountryManager();
        this.stateManager = new ServerStateManager();

        Scenario random = scenarioManager.getRandom();
        if(random != null) this.scenarioLoader.loadNew(random);
    }

    private void registerListeners() {
        core.getListenerRegistry().register(new ServerPacketListener());
    }

    public static Server getInstance() {
        return instance;
    }

    public SharedCore getCore() {
        return core;
    }

    public SendTool getSendTool() {
        return core.getTool();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public boolean isDebug() {
        return config.isDebug();
    }

    public MainConfigUtil getConfig() {
        return config;
    }

    public ServerScenarioManager getScenarioManager() {
        return scenarioManager;
    }

    public ServerCountryManager getCountryManager() {
        return countryManager;
    }

    public ServerScenarioSceneLoader getScenarioSceneLoader() {
        return scenarioLoader;
    }

    public MapTracker getMapTracker() {
        return mapTracker;
    }

    public ServerStateManager getStateManager() {
        return stateManager;
    }

    public StateValidator getStateValidator() {
        return stateValidator;
    }

    public ServerPlayerManager getPlayerManager() {
        return playerManager;
    }
}
