package de.idiotischer.bob.state;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class StateManager implements StateResolver {

    private final Set<State> statesSet = new HashSet<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();
    private boolean switchMM;

    public StateManager() {
        //reload();
    }

    public CompletableFuture<Void>  reload() {
        awaitingFuture = new CompletableFuture<>();

        statesSet.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.STATES_SYNC, ""));

        return awaitingFuture;
    }

    public boolean isSwitchMM() {
        return switchMM;
    }

    public void setSwitchMM(boolean switchMM) {
        this.switchMM = switchMM;
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
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
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

        BOB.getInstance().getTileManager().colorAllDefault(); //TODO: gucken ob man das hier für immer lassen kann

        switchMM = true;
        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public State registerState(State state) {
        statesSet.remove(state);

        statesSet.add(state);

        return state;
    }


    public List<State> getStates() {
        return statesSet
                .stream()
                .sorted(Comparator.comparing(State::abbreviation))
                .toList();
    }

    @Override
    public State resolve(String abbreviation) {
        return null;
    }

    public CompletableFuture<Void> getAwaitingFuture() {
        return awaitingFuture;
    }
}
