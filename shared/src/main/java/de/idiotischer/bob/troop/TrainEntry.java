package de.idiotischer.bob.troop;

import java.util.UUID;

public class TrainEntry {

    private int progress;
    private final UUID uuid;
    private final String troopName;
    private final String troopTemplate;
    private final int troopTrainDays;

    //progress is as long as troopTrainDays, so NOT 100 xD
    public TrainEntry(int progress, UUID uuid, String troopName, String troopTemplate, int troopTrainDays) {
        this.progress = progress;
        this.uuid = uuid;
        this.troopName = troopName;
        this.troopTemplate = troopTemplate;
        this.troopTrainDays = troopTrainDays;
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

    public String getTroopName() {
        return troopName;
    }

    public String getTroopTemplate() {
        return troopTemplate;
    }
}
