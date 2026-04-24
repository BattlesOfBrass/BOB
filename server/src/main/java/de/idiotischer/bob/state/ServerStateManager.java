package de.idiotischer.bob.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.ScenariosSyncPacket;
import de.idiotischer.bob.networking.packet.impl.StatesSyncPacket;
import de.idiotischer.bob.util.PosUtil;

import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ServerStateManager implements StateResolver{

    private final Set<State> stateSet = new HashSet<>();

    //MUSS nach CountryManager initialisiert werden sonst BOOM
    public ServerStateManager() {
    }

    public void reload() {
        stateSet.clear();

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
                        points.add(new Point(
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1])
                        ));
                    }

                }
                /*else if (locationsElement != null && locationsElement.isJsonPrimitive()) {

                    String pointsRaw = locationsElement.getAsString();

                    for (String p : pointsRaw.split("\\|")) {
                        String[] coords = p.split("[,;]");
                        points.add(new Point(
                                Integer.parseInt(coords[0]),
                                Integer.parseInt(coords[1])
                        ));
                    }

                }*/
                else if(locationsElement != null && locationsElement.isJsonPrimitive()) {
                    String pointsS = locationsElement.getAsString();

                    String[] coords = pointsS.split("[,;]");

                    coords[0] = coords[0].trim();
                    coords[1] = coords[1].trim();

                    points.add(new Point(
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1])
                    ));
                }
                else {
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

                if(Server.getInstance().isDebug())
                    System.out.println("registered state: " + state.getName()
                        + " (" + state.getAbbreviation() + ") points: "
                        + state.getPoints()
                        + " controller: "
                        + (state.getController() == null ? "none" : state.getController().getAbbreviation())
                );

            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), StatesSyncPacket.fromStates(stateSet));
    }

    public State registerState(State state) {
        stateSet.remove(state);

        stateSet.add(state);

        return state;
    }

    public List<String> getStates() {
        return stateSet.stream().map(State::toString).collect(Collectors.toList());
    }

    //public State getStateAt(int x, int y) {
    //    List<Point> points = PosUtil.getPossiblePos(BOB.getInstance().getMainRenderer().getMap(), x, y);
    //    Map<Point, State> statePoints = getStateSet().stream()
    //            .filter(Objects::nonNull)
    //            .collect(Collectors.toMap(
    //                    s -> new Point(s.getX(), s.getY()),
    //                    Function.identity(),
    //                    (a, b) -> a
    //            ));
    //    Optional<Point> point = points.stream().filter(statePoints::containsKey).findFirst();

    //    return statePoints.get(point.orElse(null));
    //}

    //TODO: optimize
    public State getStateAt(int x, int y) {
        Point click = new Point(x, y);

        for (State state : stateSet) {
            if (state == null || state.getPoints() == null) continue;

            Set<Point> expandedPoints = new HashSet<>();

            for (Point basePoint : state.getPoints()) {
                List<Point> possible = PosUtil.getPossiblePos(
                        Server.getInstance().getScenarioSceneLoader().getTakenColors(),
                        Server.getInstance().getScenarioSceneLoader().getMap(),
                        basePoint.x,
                        basePoint.y
                );

                expandedPoints.addAll(possible);
            }

            if (expandedPoints.contains(click)) {
                return state;
            }
        }

        return null;
    }

    //private boolean isPointInState(Point p, List<Point> polygon) {
    //    boolean result = false;
    //    for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
    //        int xi = polygon.get(i).x;
    //        int yi = polygon.get(i).y;
    //        int xj = polygon.get(j).x;
    //        int yj = polygon.get(j).y;
    //        boolean intersect = ((yi > p.y) != (yj > p.y)) &&
    //                (p.x < (double) (xj - xi) * (p.y - yi) / (double) (yj - yi) + xi);
    //        if (intersect) {
    //            result = !result;
    //        }
    //    }
    //    return result;
    //}

    public Set<State> getStateSet() {
        return stateSet;
    }

    @Override
    public State byAbbreviation(String abbreviation) {
        return stateSet.stream().filter(s -> s.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public State fromPos(int x, int y) {
        return getStateAt(x,y);
    }
}
