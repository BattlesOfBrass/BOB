package de.idiotischer.bob.game;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.impl.TimeCheckAndCorrectPacket;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.ArtificalPlayer;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.time.TimeResolver;
import de.idiotischer.bob.troop.ServerTroopManager;
import it.unimi.dsi.fastutil.Pair;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ServerGameManager implements TimeResolver {

    private final ScheduledExecutorService timeService = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

    private GameState state = GameState.PAUSED;
    private int speed = 1;

    private long lastUpdate;
    private long timeTicks; // in minutes

    private ScheduledFuture<?> nextTick;
    private volatile long generation;
    private long nextDeadline;

    private static final long SYNC_INTERVAL = 10; //we sync every day so why even bother xd

    private long lastChangedTime;
    private Player whoChanged;

    private final Map<String, Pair<Long, Consumer<Long>>> timeHook = new HashMap<>();

    public boolean begun = false;
    private boolean canChangeState = true;

    public void begin() {
        clear();
        restartChain();

        if (!begun) {
            begun = true;

            timeService.scheduleAtFixedRate(() -> {
                if (state == GameState.INGAME) broadcastTimeSync();
            }, SYNC_INTERVAL, SYNC_INTERVAL, TimeUnit.SECONDS);
        }

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(),new ReplyPacket(Type.TIME_RESTART, getDate() != null ? getDate().getTime() : -1L));
    }

    private synchronized void restartChain() {
        if (nextTick != null) nextTick.cancel(false);

        generation++;
        nextDeadline = System.nanoTime();

        scheduleNext(generation);
    }

    private synchronized void scheduleNext(long gen) {
        if (timeService.isShutdown() || gen != generation) return;

        long now = System.nanoTime();
        nextDeadline += SharedCore.getTickDelay(speed);
        if (nextDeadline < now - TimeUnit.SECONDS.toNanos(1)) nextDeadline = now;

        nextTick = timeService.schedule(() -> {
            if (gen != generation) return;

            try {
                tickTime();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally { scheduleNext(gen); }
        }, Math.max(0, nextDeadline - now), TimeUnit.NANOSECONDS);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.setState(state,null, false);
    }

    public void setState(GameState state, boolean bypassChange) {
        this.setState(state,null, bypassChange);
    }

    public void setState(GameState state, Player whoChanged, boolean bypassChange) {
        if(!bypassChange && !canChangeState) return;
        if(state != GameState.INGAME && state != GameState.PAUSED) clear();

        if(state == GameState.INGAME) {
            if(this.state == GameState.PAUSED && whoChanged != null) {
                if(Server.getInstance().getPlayerManager().getPlayers().size() > 1) {
                    int delay = SharedCore.getPauseDelay(); // 20s

                    if(whoChanged != this.whoChanged && System.currentTimeMillis() - lastChangedTime < delay) return;
                }
            }
        }

        this.lastChangedTime = System.currentTimeMillis();
        this.whoChanged = whoChanged;

        this.state = state;
        restartChain();

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
        timeTicks = 0;
        lastUpdate = System.nanoTime();
        timeHook.clear();
        setSpeed(1);
    }

    public void tickTime() {
        if (state != GameState.INGAME) {
            lastUpdate = System.nanoTime();
            return;
        }

        timeTicks++;

        exec.execute(this::process);
    }

    private void process() {
        try {
            new ArrayList<>(timeHook.entrySet()).forEach(e -> {
                if (timeTicks >= e.getValue().key()) {
                    timeHook.remove(e.getKey());
                    try { e.getValue().value().accept(timeTicks); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });

            ServerTroopManager tm = Server.getInstance().getTroopManager();

            new ArrayList<>(tm.getTraining()).forEach(entry -> {
                double missing = entry.getMissingPop();

                double drawn = missing > 0 ? Server.getInstance().getCountryManager().removePopulationCheck(missing, entry.getFinalStack().getController()) : 0;

                if (entry.canTrainServer(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(), drawn) && entry.isFullyPaid() && timeTicks >= entry.getEndTick()) tm.finishTraining(entry);
            });

            //TODO for every troop update them and add equipment and population, whatever they miss and you have

            Server.getInstance().getCountryManager().getCountries().forEach(c -> {
                if (timeTicks % (60 * 24) == 0) {
                    try {
                        double total = Server.getInstance().getCountryManager().getTotalPopulation(c);
                        c.newDay(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(), c.getDailyManpower(total), (long) total);
                        Server.getInstance().getCountryManager().addPopPercent(0.0041F /*like hoi4*/, c);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    /*if (timeTicks % (60 * 12) == 0)*/
                    broadcastTimeSync();
                }
                if(c.getPlayer() instanceof ArtificalPlayer pl) pl.think();
            });
        } catch (Throwable t) {
            t.printStackTrace();
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

        if(Server.getInstance().getTroopManager() != null) Server.getInstance().getTroopManager().rescheduleAllTroopMovements();

        restartChain();
        broadcastTimeSync();
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    public void reload() {
        if(!begun) begin();

        clear();
        restartChain();
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

    public void setCanChangeState(boolean b) {
        this.canChangeState = b;
    }
}