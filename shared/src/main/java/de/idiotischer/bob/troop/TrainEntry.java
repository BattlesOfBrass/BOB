package de.idiotischer.bob.troop;

import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TrainEntry {

    private final long startTime;
    private int progress;
    private final UUID uuid;
    private final int troopTrainDays;
    private TroopStack finalStack;

    public long extra = 0;

    //progress is as long as troopTrainDays, so NOT 100 xD
    public TrainEntry(long startTime, int progress, UUID uuid,int troopTrainDays, TroopStack finalStack) {
        this.startTime = startTime;
        this.progress = progress;
        this.uuid = uuid;
        this.troopTrainDays = troopTrainDays;
        this.finalStack = finalStack;
    }

    public TroopStack getFinalStack() {
        return finalStack;
    }

    public int getExpIfDeployedNow(long now) {
        long elapsedTicks = Math.max(0, now - startTime - extra);

        double elapsedDays = elapsedTicks / 1440.0;
        double totalProgress = progress + elapsedDays;
        double percentage = troopTrainDays <= 0 ? 100 : (totalProgress / troopTrainDays) * 100;

        return Math.clamp((int) Math.floor(percentage), 0, 100);
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getTroopTrainDays() {
        return troopTrainDays;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getTroopName() {
        return finalStack.getName();
    }

    public String getTroopTemplate() {
        return finalStack.getTemplate();
    }

    public boolean canTrain() {
        //blabla
        boolean result = true;

        if(!result) extra++;

        return result;
    }

    public long getEndTick() {
        return startTime + TimeUnit.DAYS.toMinutes(troopTrainDays) + extra;
    }
}
