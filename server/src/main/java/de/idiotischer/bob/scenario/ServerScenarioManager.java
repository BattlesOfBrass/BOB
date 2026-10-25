package de.idiotischer.bob.scenario;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.networking.packet.impl.ScenariosSyncPacket;
import de.idiotischer.bob.util.FileUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ServerScenarioManager {

    private final Set<Scenario> scenarios = new HashSet<>();

    public ServerScenarioManager() {
        reload();
    }

    public void reload() {
        scenarios.clear();

        List<Path> paths = FileUtil.getAllScenarios();

        paths.forEach(p -> {
            String dirName = p.getFileName().toString();

            if (Server.getInstance().isDebug() || (!dirName.equals("default") && !dirName.endsWith("_"))) {
                /*vorerst abbreviation halt nur der dir name anstatt von ner config zu holen*/
                registerScenario(new Scenario(false, dirName, dirName, p));
            }
        });

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), ScenariosSyncPacket.fromScenarios(scenarios));
    }

    public Scenario getScenario(Path path) {
        return scenarios.stream().filter(s -> s.getDir().equals(path)).findFirst().orElse(null);
    }

    public Scenario getScenario(String abbreviation) {
        return scenarios.stream().filter(s -> s.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    public Scenario registerScenario(Scenario scenario) {
        scenarios.remove(scenario);
        scenarios.add(scenario);
        return scenario;
    }

    public Set<Scenario> getScenarios() {
        return scenarios;
    }

    public Scenario getRandom() {
        Scenario[] scens = scenarios.toArray(new Scenario[0]);

        int n = ThreadLocalRandom.current().nextInt(scenarios.size());

        return scens[n];
    }
}