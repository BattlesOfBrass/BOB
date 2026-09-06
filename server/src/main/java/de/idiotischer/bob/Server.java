package de.idiotischer.bob;

import de.idiotischer.bob.combat.ServerCombatManager;
import de.idiotischer.bob.conference.ServerConferenceManager;
import de.idiotischer.bob.country.ServerCountryManager;
import de.idiotischer.bob.listener.ServerPacketListener;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.player.ServerPlayerManager;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ServerScenarioManager;
import de.idiotischer.bob.scenario.ServerScenarioSceneLoader;
import de.idiotischer.bob.state.ServerStateManager;
import de.idiotischer.bob.tile.ServerTileManager;
import de.idiotischer.bob.tile.TileValidator;
import de.idiotischer.bob.troop.ServerTroopManager;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;
import de.idiotischer.bob.war.ServerWarManager;

public class Server {

    private static Server instance;
    private final SharedCore core = new SharedCore();
    private ServerSocket serverSocket;
    private MainConfigUtil config;
    private ServerScenarioManager scenarioManager;
    private ServerCountryManager countryManager;
    private ServerScenarioSceneLoader scenarioLoader;
    private ServerTileManager tileManager;
    private TileValidator tileValidator = new TileValidator();
    private ServerPlayerManager playerManager;
    private ServerStateManager stateManager;
    private ServerTroopManager troopManager;
    private ServerWarManager warManager;
    private ServerCombatManager combatManager;
    private ServerConferenceManager conferenceManager;

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

        this.playerManager = new ServerPlayerManager();

        this.scenarioManager = new ServerScenarioManager();

        this.scenarioManager.reload();

        this.scenarioLoader = new ServerScenarioSceneLoader();

        this.countryManager = new ServerCountryManager();

        this.tileManager = new ServerTileManager();

        this.stateManager = new ServerStateManager();

        this.troopManager = new ServerTroopManager();

        this.warManager = new ServerWarManager();

        this.combatManager = new ServerCombatManager();

        this.conferenceManager = new ServerConferenceManager();

        Scenario random = scenarioManager.getRandom();
        if(random != null) this.scenarioLoader.loadNew(random);
    }

    private void registerListeners() {
        core.getListenerRegistry().register(new ServerPacketListener());
    }

    public static Server getInstance() {
        return instance;
    }

    public SharedCore getSharedCore() {
        return core;
    }

    public SendTool getSendTool() {
        return core.getTool();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public ServerStateManager getStateManager() {
        return stateManager;
    }

    public boolean isDebug() {
        return config.isDebug();
    }

    public MainConfigUtil getConfig() {
        return config;
    }

    public ServerTroopManager getTroopManager() {
        return troopManager;
    }

    public ServerCombatManager getCombatManager() {
        return combatManager;
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

    public ServerTileManager getTileManager() {
        return tileManager;
    }

    public TileValidator getTileValidator() {
        return tileValidator;
    }

    public ServerWarManager getWarManager() {
        return warManager;
    }

    public ServerPlayerManager getPlayerManager() {
        return playerManager;
    }

    public ServerConferenceManager getConferenceManager() {
        return conferenceManager;
    }
}
