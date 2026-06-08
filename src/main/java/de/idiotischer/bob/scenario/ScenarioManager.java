package de.idiotischer.bob.scenario;

import de.idiotischer.bob.BOB;
//import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.nio.file.Path;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ScenarioManager {

    private final Set<Scenario> scenarios = new HashSet<>();
    private CompletableFuture<Void> loadFuture;

    public ScenarioManager() {
        //reload();
    }

    public CompletableFuture<Void> reload() {
        loadFuture = new CompletableFuture<>();

        BOB.getInstance().getSendTool().send(
                BOB.getInstance().getClient().getChannel(),
                new RequestPacket(Type.SCENARIOS,"")
        );

        return loadFuture;
    }

    public void refreshAddNew(Scenario scenario) {
        scenarios.add(scenario);
        scenarios.forEach(this::registerScenario);

        if (loadFuture != null && !loadFuture.isDone()) {
            loadFuture.complete(null);
        }

        //BOB.getInstance().getAwaitingReload().complete(null);
    }

    public void refresh(List<Scenario> scenarios) {
        this.scenarios.clear();

        scenarios.forEach(this::registerScenario);

        if (loadFuture != null && !loadFuture.isDone()) {
            loadFuture.complete(null);
        }

        //BOB.getInstance().getAwaitingReload().complete(null);
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

    public List<Scenario> getScenariosSorted() {
        //return scenarios.stream().sorted(Collator.getInstance()).collect(Collectors.toSet());
        return scenarios.stream().sorted(Comparator.comparing(Scenario::getAbbreviation)).toList();
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
