package de.idiotischer.bob.time;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2BooleanSortedMap;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TimeManager implements TimeResolver {
    private final ScheduledExecutorService timeService = Executors.newSingleThreadScheduledExecutor();

    private int speed = 1;
    private long timeTicks;

    private Date scenarioStartDate;
    private ScheduledFuture<?> nextTick;
    private Map<String, Pair<Long,Consumer<Long>>> timeHook = new HashMap<>();

    public TimeManager() {
    }

    public void begin(Date scenarioStartDate) {
        this.scenarioStartDate = new Date(scenarioStartDate.getTime());
        if(BOB.getInstance().isDebug()) System.out.println("beginning ticking");
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        if (timeService.isShutdown()) return;

        nextTick = timeService.schedule(() -> {
            try {
                tickTime();
            } finally {
                scheduleNextTick();
            }
        }, SharedCore.getTickDelay(speed), TimeUnit.NANOSECONDS);
    }

    private void rescheduleNextTick() {
        if (nextTick != null) nextTick.cancel(false);

        scheduleNextTick();
    }

    private void tickTime() {
        if (BOB.getInstance().getGameManager().getGameState() != GameState.INGAME) return;
        BOB.getInstance().getMainRenderer().getGamePanel().getHud().refreshTrainingList();

        new ArrayList<>(BOB.getInstance().getTroopManager().getTraining()).forEach(troop -> {
            new ArrayList<>(timeHook.entrySet()).forEach(e -> {
                if (timeTicks >= e.getValue().key()) {
                    timeHook.remove(e.getKey());
                    try { e.getValue().value().accept(timeTicks); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });

            if(troop.canTrain())
                if (timeTicks >= troop.getEndTick()) {
                    BOB.getInstance().getTroopManager().removeTrainEntry(troop.getUuid(), true);
                    BOB.getInstance().getTroopManager().addTroopStack(troop.getFinalStack(), troop.getUuid());
                }
        });

        timeTicks++;
    }

    public void sync(Date startDate, long timeTicks, int speed) {
        this.scenarioStartDate = startDate;
        this.timeTicks = Math.max(0, timeTicks);
        this.speed = Math.clamp(speed, 1, 5);

        rescheduleNextTick();
    }

    public int getSpeed() {
        return Math.clamp(speed, 1, 5);
    }

    public void setSpeed(int speed) {
        int newSpeed = Math.clamp(speed, 1, 5);

        RequestPacket pack = new RequestPacket(Type.GAMESPEED_CHANGE, newSpeed);

        BOB.getInstance().getSharedCore().getTool().send(BOB.getInstance().getClient().getChannel(),pack);
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    public Date getDate() {
        Date startDate = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getStartDate();

        return new Date(startDate.getTime() + TimeUnit.MINUTES.toMillis(timeTicks));
    }

    public void reload(Date scenarioStartDate) {
        this.scenarioStartDate = new Date(scenarioStartDate.getTime());
        this.timeTicks = 0;
        this.speed = 1;

        rescheduleNextTick();
    }

    public void shutdown() {
        timeService.shutdownNow();
    }

    @Override
    public void addTimeHook(String id, long days, Consumer<Long> callback) {
        long now = timeTicks;

        long goal = now + TimeUnit.DAYS.toMinutes(days); //convert to minutes

        timeHook.put(id, Pair.of(goal, callback));
    }

    @Override
    public void removeTimeHook(String s) {

    }
}