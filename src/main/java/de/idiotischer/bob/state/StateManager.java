package de.idiotischer.bob.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.BOB;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.map.FloodFill;

import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class StateManager {

    private final Set<State> stateSet = new HashSet<>();

    //MUSS nach CountryManager initialisiert werden sonst BOOM
    public StateManager() {
        reload();
    }

    private void reload() {
        stateSet.clear();

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getStatesConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String abbreviation = entry.getKey();

                JsonObject stateElement = entry.getValue().getAsJsonObject();

                String controllerString = stateElement.get("controller").getAsString();

                Country country = Country.fromAbbreviation(controllerString);

                int x = stateElement.get("x").getAsInt();
                int y = stateElement.get("y").getAsInt();

                String name = stateElement.get("name").getAsString();

                State state = new State(abbreviation, name, x, y, country);

                registerState(state);

                System.out.println("registered state: " + state.getName()
                        + " (" + state.getAbbreviation() + ") at: "
                        + state.getX() + ";"
                        + state.getY() + " with controller: "
                        + state.getController()
                );
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void syncStates() {
    }

    public State registerState(State state) {
        stateSet.remove(state);

        stateSet.add(state);

        return state;
    }

    public List<String> getStates() {
        return stateSet.stream().map(State::toString).collect(Collectors.toList());
    }

    public State getStateAt(int x, int y) {
        List<Point> points = FloodFill.getPossiblePos(BOB.getInstance().getMainRenderer().getGamePanel().getFrame(), x, y);
        Map<Point, State> statePoints = getStateSet().stream().collect(Collectors.toMap(s -> new Point(s.getX(), s.getY()), s -> s));

        Optional<Point> point = points.stream().filter(statePoints::containsKey).findFirst();

        return statePoints.get(point.orElse(null));
    }

    public Set<State> getStateSet() {
        return stateSet;
    }
}
