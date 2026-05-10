package de.idiotischer.bob.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.StatesSyncPacket;
import de.idiotischer.bob.util.PosUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//TODO: optimize with parralel streaming and stuff
public class ServerStateManager implements StateResolver {

    private final Set<State> stateSet = new HashSet<>();

    private final Map<State, Set<Point>> cache = new HashMap<>();

    private final ExecutorService cacheExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ServerStateManager() {
    }

    public void reload() {
        stateSet.clear();
        cache.clear();

        try (JsonReader reader = new JsonReader(
                Files.newBufferedReader(
                        Server.getInstance()
                                .getScenarioSceneLoader()
                                .getCurrentScenario()
                                .getStatesConfig()
                )
        )) {

            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String abbreviation = entry.getKey();
                JsonObject stateElement = entry.getValue().getAsJsonObject();

                String controllerString = stateElement.get("controller").getAsString();
                Country country = Server.getInstance()
                        .getCountryManager()
                        .fromAbbreviation(controllerString);

                String name = stateElement.get("name").getAsString();

                List<Point> points = new ArrayList<>();
                JsonElement locationsElement = stateElement.get("locations");

                if (locationsElement != null && locationsElement.isJsonArray()) {
                    for (JsonElement el : locationsElement.getAsJsonArray()) {
                        String[] coords = el.getAsString().split("[,;]");
                        coords[0] = coords[0].trim();
                        coords[1] = coords[1].trim();
                        points.add(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
                    }
                } else if (locationsElement != null && locationsElement.isJsonPrimitive()) {
                    String[] coords = locationsElement.getAsString().split("[,;]");
                    coords[0] = coords[0].trim();
                    coords[1] = coords[1].trim();
                    points.add(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
                } else {
                    points.add(new Point(
                            stateElement.get("x").getAsInt(),
                            stateElement.get("y").getAsInt()
                    ));
                }

                State state = new State(
                        Server.getInstance().getCore(),
                        abbreviation,
                        name,
                        points,
                        country
                );

                registerState(state);

                cache(state, state.getPoints());

                if (Server.getInstance().isDebug()) {
                    System.out.println("registered state: " + state.getName()
                            + " (" + state.getAbbreviation() + ") points: "
                            + state.getPoints()
                            + " controller: "
                            + (state.getController() == null ? "none" : state.getController().getAbbreviation())
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        Server.getInstance().getSendTool().broadcast(
                Server.getInstance().getServerSocket().getClients(),
                StatesSyncPacket.fromStates(stateSet)
        );
    }

    private void cache(State state, List<Point> points) {
        if (cache.containsKey(state)) return;

        cacheExecutor.submit(() -> {
            if (Server.getInstance().isDebug()) System.out.println(state.getAbbreviation() + " caching started!");

            Set<Point> pointsSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

            List<Color> takenColors = Server.getInstance().getScenarioSceneLoader().getTakenColors();
            BufferedImage logicMap = Server.getInstance().getScenarioSceneLoader().getMap();

            points.parallelStream().forEach(basePoint -> {
                pointsSet.add(basePoint);
                List<Point> expanded = PosUtil.getPossiblePos(takenColors, logicMap, basePoint.x, basePoint.y);
                pointsSet.addAll(expanded);
            });

            synchronized (cache) {
                cache.put(state, pointsSet);
            }

            if (Server.getInstance().isDebug()) System.out.println(state.getAbbreviation() + " caching finished!");
        });
    }

    public State registerState(State state) {
        stateSet.remove(state);
        stateSet.add(state);
        cache.remove(state);
        return state;
    }

    public List<String> getStates() {
        return stateSet.stream().map(State::toString).collect(Collectors.toList());
    }

    public State getStateAt(int x, int y) {
        Point click = new Point(x, y);

        for (State state : stateSet) {
            if (state == null || state.getPoints() == null) continue;

            Set<Point> expandedPoints = cache.get(state);
            if (expandedPoints == null) {
                cache(state, state.getPoints());
                continue;
            }

            if (expandedPoints.contains(click)) {
                return state;
            }
        }

        return null;
    }


    public Set<State> getStateSet() {
        return stateSet;
    }

    @Override
    public State byAbbreviation(String abbreviation) {
        return stateSet.stream().filter(s -> s.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public State fromPos(int x, int y) {
        return getStateAt(x, y);
    }
}