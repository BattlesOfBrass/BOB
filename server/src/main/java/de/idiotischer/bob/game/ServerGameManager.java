package de.idiotischer.bob.game;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.networking.packet.impl.TimeCheckAndCorrectPacket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerGameManager {

    private final ScheduledExecutorService timeService = Executors.newSingleThreadScheduledExecutor();

    private GameState state = GameState.PAUSED;
    private int speed = 1;
    private long timeTicks; //in minutes

    public ServerGameManager() {
        clear();
        scheduleNextTick();
    }

    private void scheduleNextTick() {
        //TODO: resync tume sometimes so people arent behind (like correction packets ig)
        timeService.schedule(() -> {
            try {
                tickTime();
            } finally {
                scheduleNextTick();
            }
        }, getTickDelay(), TimeUnit.MILLISECONDS);
    }

    private long getTickDelay() {
        return 250L * getSpeed();
    }

    private void clear() {
        speed = 1;
        timeTicks = 0;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public void clientConnectTimeSync(AsynchronousSocketChannel ch) {

    }

    public void tickTime() {
        if(state == GameState.PAUSED || state == GameState.WAITING || state == GameState.LOBBY) return;

        timeTicks++;

        if (timeTicks % 40 == 0) broadcastTimeSync();
    }

    public void broadcastTimeSync() {
        TimeCheckAndCorrectPacket packet = new TimeCheckAndCorrectPacket(timeTicks, speed);
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(), packet);
    }

    public Date getDate() {
        Date startDate = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getStartDate();

        return new Date(startDate.getTime() + TimeUnit.MINUTES.toMillis(timeTicks));
    }

    public int getSpeed() {
        return Math.clamp(speed, 1, 5);
    }

    public void setSpeed(int speed) {
        this.speed = Math.clamp(speed, 1, 5);

        if (state == GameState.INGAME) broadcastTimeSync();
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    public void reload() {
        clear();
    }
}
