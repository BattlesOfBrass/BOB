
package de.idiotischer.bob.game;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.impl.TimeCheckAndCorrectPacket;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.time.TimeResolver;
import it.unimi.dsi.fastutil.Pair;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ServerGameManager implements TimeResolver {

    private final ScheduledExecutorService timeService = Executors.newSingleThreadScheduledExecutor();

    private GameState state = GameState.PAUSED;
    private int speed = 1;

    private long lastUpdate;
    private long timeTicks; // in minutes

    private ScheduledFuture<?> nextTick;

    private static final long SYNC_INTERVAL = 10; //we sync every day so why even bother xd

    private long lastChangedTime;
    private Player whoChanged;

    private final Map<String, Pair<Long, Consumer<Long>>> timeHook = new HashMap<>();

    public void begin() {
        clear();
        scheduleNextTick();

        timeService.scheduleAtFixedRate(() -> {
            if (state == GameState.INGAME) broadcastTimeSync();
        }, SYNC_INTERVAL, SYNC_INTERVAL, TimeUnit.SECONDS);
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

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.setState(state,null);
    }

    public void setState(GameState state, Player whoChanged) {
        if(state != GameState.INGAME && state != GameState.PAUSED) begin();

        if(state == GameState.INGAME) {
            if(this.state == GameState.PAUSED && whoChanged != null) {
                //if we are not in singleplayer
                if(Server.getInstance().getPlayerManager().getPlayers().size() > 1) {
                    int delay = SharedCore.getPauseDelay(); // 20s

                    //and another person tries to unpause and the delay hasn't passed dont change the state
                    if(whoChanged != this.whoChanged && System.currentTimeMillis() - lastChangedTime < delay) return;
                }
            }

            scheduleNextTick();
        }

        this.lastChangedTime = System.currentTimeMillis();
        this.whoChanged = whoChanged;

        this.state = state;
        rescheduleNextTick();

        broadcastTimeSync();
        broadcastStateChange();
    }

    private void broadcastStateChange() {
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(), new ReplyPacket(Type.GAMESTATE_SYNC, state.name()));
    }

    public void clientConnectTimeSync(AsynchronousSocketChannel ch) {
        // TODO: Send a time sync packet to this client directly
    }

    private void clear() {
        speed = 1;
        timeTicks = 0;
        lastUpdate = System.nanoTime();
    }

    public void tickTime() {
        if (state != GameState.INGAME) {
            lastUpdate = System.nanoTime();
            return;
        }

        timeTicks++;

        timeHook.forEach((key, keyValue) -> {
            if (timeTicks >= keyValue.key()) keyValue.value().accept(timeTicks);
        });

        if (timeTicks % (60 * 24) == 0) {
            Server.getInstance().getCountryManager().getCountries().forEach(c -> c.newDay(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool()));
        /*if (timeTicks % (60 * 12) == 0)*/
            broadcastTimeSync();
        }
    }

    public void broadcastTimeSync() {
        lastUpdate = System.nanoTime();

        TimeCheckAndCorrectPacket packet = new TimeCheckAndCorrectPacket(timeTicks, speed, Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getStartDate());

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

        Server.getInstance().getTroopManager().rescheduleAllTroopMovements();

        rescheduleNextTick();
        broadcastTimeSync();
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    public void reload() {
        clear();
        rescheduleNextTick();
        broadcastTimeSync();
    }

    @Override
    public void addTimeHook(String id, long days, Consumer<Long> callback) {
        long now = timeTicks;

        long goal = now + TimeUnit.DAYS.toMinutes(days); //convert to minutes

        timeHook.put(id, Pair.of(goal, callback));
    }

    @Override
    public void removeTimeHook(String s) {
        timeHook.remove(s);
    }

    public Map<String, Pair<Long, Consumer<Long>>> getTimeHook() {
        return timeHook;
    }
}
