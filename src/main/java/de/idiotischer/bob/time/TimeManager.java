package de.idiotischer.bob.time;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import javax.swing.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimeManager {
    private final ScheduledExecutorService timeService = Executors.newSingleThreadScheduledExecutor();

    private int speed = 1;
    private long timeTicks;

    private Date scenarioStartDate;
    private ScheduledFuture<?> nextTick;

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
        }, getTickDelay(), TimeUnit.NANOSECONDS);
    }

    private void rescheduleNextTick() {
        if (nextTick != null) nextTick.cancel(false);

        scheduleNextTick();
    }

    private long getTickDelay() {
        return SharedCore.getTickNanos() / getSpeed();
    }

    private void tickTime() {
        if (BOB.getInstance().getGameManager().getGameState() != GameState.INGAME) return;

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

        if (this.speed == newSpeed) return;

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
}