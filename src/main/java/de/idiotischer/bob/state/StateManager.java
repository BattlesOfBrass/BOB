package de.idiotischer.bob.state;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.map.FloodFill;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.util.PosUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static de.idiotischer.bob.util.ImageUtil.deepCopy;

public class StateManager implements StateResolver {

    private final Set<State> stateSet = new HashSet<>();
    private CompletableFuture<Void> awaitingFuture;
    private boolean switchMM = true;

    //MUSS nach CountryManager initialisiert werden sonst BOOM
    public StateManager() {
        //reload();
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        stateSet.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.STATES_SYNC,""));

        return awaitingFuture;
    }

    public boolean has(String stateAbbreviation) {
        return getStateSet().stream().map(State::getAbbreviation).collect(Collectors.toSet()).contains(stateAbbreviation);
    }

    public boolean has(State state) {
        return has(state.getAbbreviation());
    }

    public void finishReload(boolean withInit) {
        if(withInit && !BOB.getInstance().isInitialized()) {
            BOB.getInstance().setup();
            switchMM = false;
        }

        if(BOB.getInstance().getMainRenderer() == null) return;

        if(switchMM) {
            BOB.getInstance().getMainRenderer().getGamePanel().setEscMenu(false);
            BOB.getInstance().getMainRenderer().setMainMenu(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelect(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new ScenarioSelectMenu(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario()));
        }

        if(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage() != null) {
            //TODO: check if i need this 2x
            BOB.getInstance().getMainRenderer().setMap(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage());//, currentScenario.getBackgroundImage());

            SwingUtilities.invokeLater(() -> {
                if( BOB.getInstance().getMainRenderer().getGamePanel() == null) return;
                BOB.getInstance().getMainRenderer().setMap(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage());//,currentScenario.getBackgroundImage());
                BOB.getInstance().getMainRenderer().getCamera().zoomToMin();
            });
        }

        BOB.getInstance().getStateManager().colorAllDefault(); //TODO: gucken ob man das hier für immer lassen kann

        switchMM = true;
        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public boolean isSwitchMM() {
        return switchMM;
    }

    public void setSwitchMM(boolean switchMM) {
        this.switchMM = switchMM;
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
                        BOB.getInstance().getScenarioSceneLoader().getTakenColors(),
                        BOB.getInstance().getMainRenderer().getMap(),
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

    public void colorAllDefault() {
        getStateSet().forEach(this::colorStateDefault);
    }

    public void colorStateDefault(State state) {
        if(state.getController() == null) return;

        SwingUtilities.invokeLater(() -> {
            recolorState(state);
            //FloodFill.fillAll(BOB.getInstance().getMainRenderer().getLogicMap(), state.getPoints(), state.getController().countryColor());
        });
    }

    public void changeState(State state, Country newOwner) {
        if(newOwner == null) return;
        SwingUtilities.invokeLater(() -> {
            recolorState(state);
            //FloodFill.fillAll(BOB.getInstance().getMainRenderer().getLogicMap(), state.getPoints(), newOwner.countryColor());
        });
    }

    //private boolean isPointInState(java.awt.Point p, List<java.awt.Point> polygon) {
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
        return Collections.unmodifiableSet(stateSet);
    }

    public static void recolorState(State state) {
        List<Color> taken = BOB.getInstance().getScenarioSceneLoader().getTakenColors();

        state.getPoints().forEach(pos -> {
            PosUtil.getPossiblePos(taken, BOB.getInstance().getMainRenderer().getLogicMap(), pos.x, pos.y).forEach(px -> {
                BOB.getInstance().getMainRenderer().getLogicMap().setRGB(px.x,px.y, state.getController().countryColor().getRGB());
            });

            BOB.getInstance().getMainRenderer().getLogicMap().setRGB(pos.x,pos.y, state.getController().countryColor().getRGB());
            BOB.getInstance().getMainRenderer().syncBuffers();
        });
    }

    public static void recolorState(State state, Color color) {
        List<Color> taken = BOB.getInstance().getScenarioSceneLoader().getTakenColors();

        state.getPoints().forEach(pos -> {
            PosUtil.getPossiblePos(taken, BOB.getInstance().getMainRenderer().getLogicMap(), pos.x, pos.y).forEach(px -> {
                BOB.getInstance().getMainRenderer().getLogicMap().setRGB(px.x,px.y, color.getRGB());
            });

            BOB.getInstance().getMainRenderer().getLogicMap().setRGB(pos.x,pos.y, color.getRGB());

            BOB.getInstance().getMainRenderer().syncBuffers();
        });
    }

    @Override
    public State byAbbreviation(String abbreviation) {
        return stateSet.stream().filter(s -> s.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public State fromPos(int x, int y) {
        return getStateAt(x,y);
    }

    public CompletableFuture<Void> getAwaitingFuture() {
        return awaitingFuture;
    }

    public void clearStates() {
        stateSet.clear();
    }
}