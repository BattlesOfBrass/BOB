package de.idiotischer.bob.scenario;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.util.FileUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ScenarioManager {

    private final Set<Scenario> scenarios = new HashSet<>();

    public ScenarioManager() {
        reload();
    }

    public void reload() {
        scenarios.clear();

        List<Path> paths = FileUtil.getAllScenarios();

        paths.forEach(p -> {
            String dirName = p.getFileName().toString();

            if (BOB.getInstance().isDebug() || (!dirName.equals("default") && !dirName.endsWith("_"))) {
                registerScenario(new Scenario(dirName, p));
            }
        });
    }

    public Scenario getScenario(Path path) {
        return scenarios.stream().filter(s -> s.getDir().equals(path)).findFirst().orElse(null);
    }

    public Scenario getScenario(String name) {
        return scenarios.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
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
